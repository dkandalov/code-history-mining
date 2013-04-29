import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diff.impl.fragments.LineFragment
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.RepositoryLocation
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.util.Consumer
import git4idea.GitUtil
import git4idea.changes.GitCommittedChangeList
import git4idea.changes.GitRepositoryLocation
import git4idea.commands.GitSimpleHandler
import org.jetbrains.annotations.Nullable

import java.text.SimpleDateFormat

import static ChangeExtractor.*
import static Measure.measure
import static ProjectHistory.fetchChangeListsFor
import static com.intellij.openapi.diff.impl.ComparisonPolicy.TRIM_SPACE
import static com.intellij.openapi.diff.impl.highlighting.FragmentSide.SIDE1
import static com.intellij.openapi.diff.impl.highlighting.FragmentSide.SIDE2
import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static intellijeval.PluginUtil.*

if (isIdeStartup) return

//new TextCompareProcessorTestSuite(project).run()
//if (true) return


doInBackground("Analyzing project history", { ProgressIndicator indicator ->
	measure("time") {
		def storage = new EventStorage("${PathManager.pluginsPath}/delta-flora/${project.name}-events.csv")

		def now = new Date()
		def daysOfHistory = 900
		def sizeOfVCSRequestInDays = 1

		if (storage.hasNoEvents()) {
			def historyStart = now - daysOfHistory
			def historyEnd = now

			log("Loading project history from $historyStart to $historyEnd")
			Iterator<CommittedChangeList> changeLists = fetchChangeListsFor(project, historyStart, historyEnd, sizeOfVCSRequestInDays)
			processChangeLists(changeLists, indicator) { changeEvents ->
				storage.appendToEventsFile(changeEvents)
			}
		} else {
			def historyStart = storage.mostRecentEventTime
			def historyEnd = now
			log("Loading project history from $historyStart to $historyEnd")

			def changeLists = fetchChangeListsFor(project, historyStart, historyEnd, sizeOfVCSRequestInDays, false)
			processChangeLists(changeLists, indicator) { changeEvents ->
				storage.prependToEventsFile(changeEvents)
			}

			historyStart = now - daysOfHistory
			historyEnd = storage.oldestEventTime
			log("Loading project history from $historyStart to $historyEnd")

			changeLists = fetchChangeListsFor(project, historyStart, historyEnd, sizeOfVCSRequestInDays)
			processChangeLists(changeLists, indicator) { changeEvents ->
				storage.appendToEventsFile(changeEvents)
			}
		}

		showInConsole("Saved change events to ${storage.filePath}", "output", project)
		showInConsole("(it should have history from '${storage.oldestEventTime}' to '${storage.mostRecentEventTime}')", "output", project)
	}
	Measure.durations.entrySet().collect{ "Total " + it.key + ": " + it.value }.each{ log(it) }
}, {})

def processChangeLists(changeLists, indicator, callback) {
	for (changeList in changeLists) {
		if (changeList == null) break
		if (indicator.canceled) break
		log(changeList.name)

		def date = dateFormat.format((Date) changeList.commitDate)
		indicator.text = "Analyzing project history (${date} - '${changeList.comment.trim()}')"
		catchingAll_ {
			Collection<ChangeEvent> changeEvents = decomposeIntoChangeEvents((CommittedChangeList) changeList, project)
			callback(changeEvents)
		}
		indicator.text = "Analyzing project history (${date} - looking for next commit...)"
	}
}

@Nullable static <T> T catchingAll_(Closure<T> closure) {
	try {
		closure.call()
	} catch (Exception e) {
		log(e)
		null
	}
}

class ProjectHistory {

	static Iterator<CommittedChangeList> fetchChangeListsFor(Project project, Date historyStart, Date historyEnd,
	                                                    int sizeOfVCSRequestInDays = 30, boolean presentToPast = true) {
		def dateIterator = (presentToPast ?
				new PresentToPastIterator(historyStart, historyEnd, sizeOfVCSRequestInDays) :
				new PastToPresentIterator(historyStart, historyEnd, sizeOfVCSRequestInDays))
		List<CommittedChangeList> changes = []

		new Iterator<CommittedChangeList>() {
			@Override boolean hasNext() {
				!changes.empty || dateIterator.hasNext()
			}

			@Override CommittedChangeList next() {
				if (!changes.empty) return changes.remove(0)

				measure("git request time") {
					while (changes.empty && dateIterator.hasNext()) {
						def dates = dateIterator.next()
						changes = requestChangeListsFor(project, dates.from, dates.to)
						if (!presentToPast) changes = changes.reverse()
					}
				}
				changes.empty ? null : changes.remove(0)
			}

			@Override void remove() {
				throw new UnsupportedOperationException()
			}
		}
	}

	private static List<CommittedChangeList> requestChangeListsFor(Project project, Date fromDate = null, Date toDate = null) {
		def sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots.toList()
		def sourceRoot = sourceRoots.first()
		def vcsRoot = ProjectLevelVcsManager.getInstance(project).getVcsRootObjectFor(sourceRoot)
		if (vcsRoot == null) return []

		def changesProvider = vcsRoot.vcs.committedChangesProvider
		def location = changesProvider.getLocationFor(FilePathImpl.create(vcsRoot.path))
		if (changesProvider.class.simpleName == "GitCommittedChangeListProvider") {
			return bug_IDEA_102084(project, location, fromDate, toDate)
		}

		def settings = changesProvider.createDefaultSettings()
		if (fromDate != null) {
			settings.USE_DATE_AFTER_FILTER = true
			settings.dateAfter = fromDate
		}
		if (toDate != null) {
			settings.USE_DATE_BEFORE_FILTER = true
			settings.dateBefore = toDate
		}
		changesProvider.getCommittedChanges(settings, location, changesProvider.unlimitedCountValue)
	}

	/**
	 * see http://youtrack.jetbrains.com/issue/IDEA-102084
	 * this issue is fixed, left this workaround anyway to have backward compatibility with IJ12 releases before the fix
	 */
	private static List<CommittedChangeList> bug_IDEA_102084(Project project, RepositoryLocation location, Date fromDate = null, Date toDate = null) {
		def result = []
		def parametersSpecifier = new Consumer<GitSimpleHandler>() {
			@Override void consume(GitSimpleHandler h) {
				// makes git notice file renames/moves (not sure but seems that otherewise intellij api doesn't do it)
				h.addParameters("-M")

				if (toDate != null) h.addParameters("--before=" + GitUtil.gitTime(toDate));
				if (fromDate != null) h.addParameters("--after=" + GitUtil.gitTime(fromDate));
			}
		}
		def resultConsumer = new Consumer<GitCommittedChangeList>() {
			@Override void consume(GitCommittedChangeList changeList) {
				result << changeList
			}
		}
		VirtualFile root = LocalFileSystem.instance.findFileByIoFile(((GitRepositoryLocation) location).root)

		// this is another difference compared to GitCommittedChangeListProvider#getCommittedChangesImpl
		// if "false", merge CommittedChangeList will contain all changes from merge which is NOT useful for this use-case
		// TODO (not sure how it works with other VCS)
		boolean skipDiffsForMerge = true

		GitUtil.getLocalCommittedChanges(project, root, parametersSpecifier, resultConsumer, skipDiffsForMerge)
		result
	}
}

class EventStorage {
	static final String CSV_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z"

	final String filePath

	EventStorage(String filePath) {
		this.filePath = filePath
	}

	List<ChangeEvent> readAllEvents() {
		def events = []
		new File(filePath).withReader { reader ->
			def line = null
			while ((line = reader.readLine()) != null) {
				try {
					events << fromCsv(line)
				} catch (Exception ignored) {
					println("Failed to parse line '${line}'")
				}
			}
		}
		events
	}

	def appendToEventsFile(Collection<ChangeEvent> changeEvents) {
		if (changeEvents.empty) return
		appendTo(filePath, toCsv(changeEvents))
	}

	def prependToEventsFile(Collection<ChangeEvent> changeEvents) {
		if (changeEvents.empty) return
		prependTo(filePath, toCsv(changeEvents))
	}

	Date getOldestEventTime() {
		def line = readLastLine(filePath)
		if (line == null) null
		else {
			// minus one second because git "before" seems to be inclusive (even though ChangeBrowserSettings API is exclusive)
			// (it means that if processing stops between two commits that happened on the same second,
			// we will miss one of them.. considered this to be insignificant)
			def date = fromCsv(line).revisionDate
			date.time -= 1000
			date
		}
	}

	Date getMostRecentEventTime() {
		def line = readFirstLine(filePath)
		if (line == null) new Date()
		else {
			def date = fromCsv(line).revisionDate
			date.time += 1000 // plus one second (see comments in getOldestEventTime())
			date
		}
	}

	boolean hasNoEvents() {
		def file = new File(filePath)
		!file.exists() || file.length() == 0
	}

	private static String toCsv(Collection<ChangeEvent> changeEvents) {
		changeEvents.collect{toCsv(it)}.join("\n") + "\n"
	}

	private static String toCsv(ChangeEvent changeEvent) {
		changeEvent.with {
			def commitMessageEscaped = '"' + commitMessage.replaceAll("\"", "\\\"").replaceAll("\n", "\\\\n") + '"'
			[format(revisionDate), revision, author, elementName.replaceAll(",", ""),
					fileName, fileChangeType, packageBefore, packageAfter, linesInFileBefore, linesInFileAfter,
					changeType, fromLine, toLine, fromOffset, toOffset, commitMessageEscaped].join(",")
		}
	}

	private static ChangeEvent fromCsv(String line) {
		def (revisionDate, revision, author, elementName,
				fileName, fileChangeType, packageBefore, packageAfter, linesInFileBefore, linesInFileAfter,
				changeType, fromLine, toLine, fromOffset, toOffset) = line.split(",")
		revisionDate = new SimpleDateFormat(CSV_DATE_FORMAT).parse(revisionDate)
		def commitMessage = line.substring(line.indexOf('"') + 1, line.size() - 1)

		def event = new ChangeEvent(
				new CommitInfo(revision, author, revisionDate, commitMessage),
				new FileChangeInfo(fileName, fileChangeType, packageBefore, packageAfter, linesInFileBefore.toInteger(), linesInFileAfter.toInteger()),
				new ElementChangeInfo(elementName, changeType, fromLine.toInteger(), toLine.toInteger(), fromOffset.toInteger(), toOffset.toInteger())
		)
		event
	}

	private static String format(Date date) {
		new SimpleDateFormat(CSV_DATE_FORMAT).format(date)
	}

	private static String readFirstLine(String filePath) {
		def file = new File(filePath)
		if (!file.exists() || file.length() == 0) return null
		file.withReader{ it.readLine() }
	}

	private static String readLastLine(String filePath) {
		def file = new File(filePath)
		if (!file.exists() || file.length() == 0) return null

		def randomAccess = new RandomAccessFile(file, "r")
		try {

			int shift = 1 // shift in case file ends with single newline
			for (long pos = file.length() - 1 - shift; pos >= 0; pos--) {
				randomAccess.seek(pos)
				if (randomAccess.read() == '\n') {
					return randomAccess.readLine()
				}
			}
			// assume that file has only one line
			randomAccess.seek(0)
			randomAccess.readLine()

		} finally {
			randomAccess.close()
		}
	}

	private static appendTo(String filePath, String text) {
		def file = new File(filePath)
		FileUtil.createParentDirs(file)
		file.append(text)
	}

	private static prependTo(String filePath, String text) {
		def tempFile = FileUtil.createTempFile("delta_flora", "_${new Random().nextInt(10000)}")
		def file = new File(filePath)

		tempFile.withOutputStream { output ->
			output.write(text.bytes)
			file.withInputStream { input ->
				// magic buffer size is copied from com.intellij.openapi.util.io.FileUtilRt#BUFFER (assume there is a reason for it)
				byte[] buffer = new byte[1024 * 20]
				while (true) {
					int read = input.read(buffer)
					if (read < 0) break
					output.write(buffer, 0, read)
				}
			}
		}

		file.delete()
		FileUtil.rename(tempFile, file)
	}
}


class ChangeExtractor {

	static Collection<ChangeEvent> decomposeIntoChangeEvents(CommittedChangeList changeList, Project project) {
		try {
			def commitInfo = commitInfoOf(changeList)
			changeList.changes.collectMany { Change change ->
				change.with {
//					long timeBeforeGettingGitContent = System.currentTimeMillis() // TODO
//					record("git content time", System.currentTimeMillis() - timeBeforeGettingGitContent)

					def fileChangeInfo = fileChangeInfoOf(change, project)
					if (fileChangeInfo == null) return []

					elementChangesOf(change, project).collect{ new ChangeEvent(commitInfo, fileChangeInfo, it) }
				}
			} as Collection<ChangeEvent>
		} catch (ProcessCanceledException ignore) {
			[]
		}
	}

	private static Collection<ElementChangeInfo> elementChangesOf(Change change, Project project) {
		change.with{
			def beforeText = withDefault("", beforeRevision?.content)
			def afterText = withDefault("", afterRevision?.content)
			def nonEmptyRevision = (afterRevision == null ? beforeRevision : afterRevision)
			if (nonEmptyRevision.file.fileType.isBinary()) return []

			def parseAsPsi = { String text ->
				runReadAction {
					def fileFactory = PsiFileFactory.getInstance(project)
					fileFactory.createFileFromText(nonEmptyRevision.file.name, nonEmptyRevision.file.fileType, text)
				} as PsiFile
			}
			elementChangesBetween(beforeText, afterText, parseAsPsi)
		}
	}

	private static FileChangeInfo fileChangeInfoOf(Change change, Project project) {
		change.with {
			def beforeText = withDefault("", beforeRevision?.content)
			def afterText = withDefault("", afterRevision?.content)
			def nonEmptyRevision = (afterRevision == null ? beforeRevision : afterRevision)
			if (nonEmptyRevision.file.fileType.isBinary()) return null

			def packageBefore = withDefault("", beforeRevision?.file?.parentPath?.path).replace(project.basePath, "")
			def packageAfter = withDefault("", afterRevision?.file?.parentPath?.path).replace(project.basePath, "")
			new FileChangeInfo(
					nonEmptyRevision.file.name,
					type.toString(),
					packageBefore,
					packageAfter == packageBefore ? "" : packageAfter,
					beforeText.split("\n").length,
					afterText.split("\n").length
			)
		} as FileChangeInfo
	}

	private static CommitInfo commitInfoOf(CommittedChangeList changeList) {
		new CommitInfo(
				revisionNumberOf(changeList),
				removeEmailFrom(changeList.committerName),
				changeList.commitDate, changeList.comment.trim()
		)
	}

	static Collection<ElementChangeInfo> elementChangesBetween(String beforeText, String afterText, Closure<PsiFile> parseToPsi) {
		PsiFile psiBefore = measure("parsing time"){ parseToPsi(beforeText) }
		PsiFile psiAfter = measure("parsing time"){ parseToPsi(afterText) }

		def changedFragments = measure("diff time") { new TextCompareProcessor(TRIM_SPACE).process(beforeText, afterText).findAll { it.type != null } }

		changedFragments.collectMany { LineFragment fragment ->
			measure("change events time") {
				def offsetToLineNumber = { int offset -> fragment.type == DELETED ? toLineNumber(offset, beforeText) : toLineNumber(offset, afterText) }

				def revisionWithCode = (fragment.type == DELETED ? psiBefore : psiAfter)
				def range = (fragment.type == DELETED ? fragment.getRange(SIDE1) : fragment.getRange(SIDE2))

				List<ElementChangeInfo> changeEvents = []
				def addChangeEvent = { PsiNamedElement psiElement, int fromOffset, int toOffset ->
					def partialChangeEvent = new ElementChangeInfo(
							fullNameOf(psiElement),
							diffTypeOf(fragment),
							offsetToLineNumber(fromOffset),
							offsetToLineNumber(toOffset),
							fromOffset,
							toOffset
					)
					changeEvents << partialChangeEvent
				}

				PsiNamedElement prevPsiElement = null
				int fromOffset = range.startOffset
				for (int offset = range.startOffset; offset < range.endOffset; offset++) {
					// running read action on fine-grained level because this seems to improve UI responsiveness
					// even though it will make the whole processing slower
					runReadAction {
						PsiNamedElement psiElement = methodOrClassAt(offset, revisionWithCode)
						if (psiElement != prevPsiElement) {
							if (prevPsiElement != null)
								addChangeEvent(prevPsiElement, fromOffset, offset)
							prevPsiElement = psiElement
							fromOffset = offset
						}
					}
				}
				runReadAction {
					if (prevPsiElement != null)
						addChangeEvent(prevPsiElement, fromOffset, range.endOffset)
				}

				changeEvents
			}
		} as Collection<ElementChangeInfo>
	}

	private static String revisionNumberOf(CommittedChangeList changeList) {
		// TODO this is a hack to get git ssh (it might be worth using VcsRevisionNumberAware but it's currently not released)
		if (changeList.class.simpleName == "GitCommittedChangeList") {
			changeList.name.with{ it[it.lastIndexOf('(') + 1..<it.lastIndexOf(')')] }
		} else {
			changeList.number.toString()
		}
	}

	private static String removeEmailFrom(String committerName) {
		committerName.replaceAll(/\s+<.+@.+>/, "").trim()
	}

	private static String diffTypeOf(LineFragment fragment) {
		// this is because if fragment has children it infers diff type from them,
		// which can be "INSERT/DELETED" event though from line point of view it is "CHANGED"
		def diffType = (fragment.childrenIterator != null ? CHANGED : fragment.type)

		switch (diffType) {
			case INSERT: return "added"
			case CHANGED: return "changed"
			case DELETED: return "deleted"
			case CONFLICT: return "other"
			case NONE: return "other"
			default: return "other"
		}
	}

	private static int toLineNumber(int offset, String text) {
		int counter = 0
		for (int i = 0; i < offset; i++) {
			if (text.charAt(i) == '\n') counter++
		}
		counter
	}

	private static String containingFileName(PsiElement psiElement) {
		if (psiElement == null) "null"
		else if (psiElement instanceof PsiFile) psiElement.name
		else (containingFileName(psiElement.parent))
	}

	private static String fullNameOf(PsiElement psiElement) {
		if (psiElement == null) "null"
		else if (psiElement instanceof PsiFile) ""
		else if (psiElement in PsiAnonymousClass) {
			def parentName = fullNameOf(psiElement.parent)
			def name = "[" + psiElement.baseClassType.className + "]"
			parentName.empty ? name : (parentName + "::" + name)
		} else if (psiElement instanceof PsiMethod || psiElement instanceof PsiClass) {
			def parentName = fullNameOf(psiElement.parent)
			parentName.empty ? psiElement.name : (parentName + "::" + psiElement.name)
		} else {
			fullNameOf(psiElement.parent)
		}
	}

	private static PsiNamedElement methodOrClassAt(int offset, PsiFile psiFile) {
		parentMethodOrClassOf(psiFile.findElementAt(offset))
	}

	private static PsiNamedElement parentMethodOrClassOf(PsiElement psiElement) {
		if (psiElement == null) null
		else if (psiElement instanceof PsiMethod) psiElement as PsiNamedElement
		else if (psiElement instanceof PsiClass) psiElement as PsiNamedElement
		else if (psiElement instanceof PsiFile) psiElement as PsiNamedElement
		else parentMethodOrClassOf(psiElement.parent)
	}

	private static String withDefault(defaultValue, value) { value == null ? defaultValue : value }
}

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
final class ChangeEvent {
	@Delegate CommitInfo commitInfo
	@Delegate FileChangeInfo fileChangeInfo
	@Delegate ElementChangeInfo partialChangeEvent
}

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class CommitInfo {
	String revision
	String author
	Date revisionDate
	String commitMessage
}

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class FileChangeInfo {
	String fileName
	String fileChangeType
	String packageBefore
	String packageAfter
	int linesInFileBefore
	int linesInFileAfter
}

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
final class ElementChangeInfo {
	String elementName
	String changeType
	int fromLine
	int toLine
	int fromOffset
	int toOffset
}

class Measure {
	static Map<String, Long> durations = [:].withDefault{ 0 }

	static <T> T measure(String id, Closure<T> closure) {
		long start = System.currentTimeMillis()
		T result = closure()
		long time = System.currentTimeMillis() - start
		durations[id] += time
		result
	}

	static record(String id, long duration) {
		durations[id] += duration
	}
}

class CurrentFileHistory {

	static showChangeEventsForCurrentFileHistory(Project project) {
		def file = currentFileIn(project)
		def (errorMessage, List<VcsFileRevision> revisions) = tryToGetHistoryFor(file, project)
		if (errorMessage != null) {
			show(errorMessage)
			return
		}
		show("good to go")

		def changeEvents = extractChangeEvents(file, revisions, project)
		showInConsole(toCsv(changeEvents.take(12)), "output", project)

		show("done")
	}

	private static tryToGetHistoryFor(VirtualFile file, Project project) {
		if (file == null) return ["Virtual file was null"]

		AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file)
		if (activeVcs == null) return ["There is no history for '${file.name}'"]

		def historySession = activeVcs.vcsHistoryProvider.createSessionFor(new FilePathImpl(file))
		def revisions = historySession.revisionList.sort{ it.revisionDate }
		if (revisions.size() < 1) return ["There are no committed revisions for '${file.name}'"]

		def noErrors = null
		[noErrors, revisions]
	}

	private static List<ChangeEvent> extractChangeEvents(VirtualFile file, List<VcsFileRevision> revisions, Project project) {
		def revisionPairs = [[null, revisions.first()]] + (0..<revisions.size() - 1).collect { revisions[it, it + 1] }
		def psiFileFactory = PsiFileFactory.getInstance(project)
		def parseAsPsi = { String text -> psiFileFactory.createFileFromText(file.name, file.fileType, text) }

		(List<ChangeEvent>) revisionPairs.collectMany { VcsFileRevision before, VcsFileRevision after ->
			def beforeText = (before == null ? "" : new String(before.content))
			def afterText = new String(after.content)
			def commitInfo = new CommitInfo(after.revisionNumber.asString(), after.author, after.revisionDate, after.commitMessage)
			ChangeExtractor.elementChangesBetween(beforeText, afterText, commitInfo, parseAsPsi)
		}
	}
}