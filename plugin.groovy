import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diff.impl.fragments.LineFragment
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
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
import com.intellij.unscramble.UnscrambleDialog
import com.intellij.util.Consumer
import git4idea.GitUtil
import git4idea.changes.GitCommittedChangeList
import git4idea.changes.GitRepositoryLocation
import git4idea.commands.GitSimpleHandler
import groovy.time.TimeCategory
import org.jetbrains.annotations.Nullable

import java.text.SimpleDateFormat

import static ChangeEvent.toCsv
import static Measure.measure
import static Measure.record
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

		def storage = new EventStorage(project.name)
		def fromDate = storage.oldestEventTime
		def toDate = (new Date() - 300)
		Iterator<CommittedChangeList> changeLists = ProjectHistory.changeListsFor(project, fromDate, toDate)
		for (changeList in changeLists) {
			if (changeList == null) break
			if (indicator.canceled) break

			catchingAll_ {
				Collection<ChangeEvent> changeEvents = extractChangeEvents((CommittedChangeList) changeList, project, indicator)
				storage.appendToEventsFile(changeEvents)
			}

			indicator.text = "Analyzing project history (looked at ${dateFormat.format((Date) changeList.commitDate)})"
		}
		showInConsole("Saved change events to ${storage.fileName}", "output", project)

	}
	Measure.durations.entrySet().collect{ "Total " + it.key + ": " + it.value }.each{ log(it) }
}, {})

static Collection<ChangeEvent> extractChangeEvents(CommittedChangeList changeList, Project project, ProgressIndicator indicator = null) {
	(Collection<ChangeEvent>) changeList.changes.collectMany { Change change ->
		if (indicator?.canceled) return []
		catchingAll_ {
			change.with {
				long before = System.currentTimeMillis()

				def beforeText = (beforeRevision == null ? "" : beforeRevision.content)
				def afterText = (afterRevision == null ? "" : afterRevision.content)
				def nonEmptyRevision = (afterRevision == null ? beforeRevision : afterRevision)
				def commitInfo = new CommitInfo(nonEmptyRevision.revisionNumber.asString(),
						changeList.committerName, changeList.commitDate, changeList.comment.trim())
				def parseAsPsi = { String text ->
					runReadAction {
						def fileFactory = PsiFileFactory.getInstance(project)
						fileFactory.createFileFromText(nonEmptyRevision.file.name, nonEmptyRevision.file.fileType, text)
					}
				}

				record("git content time", System.currentTimeMillis() - before)

				try {
					ChangeFinder.changesEventsBetween(beforeText, afterText, commitInfo, parseAsPsi)
				} catch (ProcessCanceledException ignore) {
					[]
				}

			}
		}
	}
}

@Nullable static <T> T catchingAll_(Closure<T> closure) {
	try {

		closure.call()

	} catch (Exception e) {
		def writer = new StringWriter()
		def message = e
		message.printStackTrace(new PrintWriter(writer))
		message = UnscrambleDialog.normalizeText(writer.buffer.toString())
		ProjectManager.instance.openProjects.each{ showInNewConsole(message, e.class.simpleName, it) }
		null
	}
}

class ProjectHistory {

	static Iterator<CommittedChangeList> changeListsFor(Project project, Date fromDate = new Date(), Date toDate = null) {
		use(TimeCategory) {
			List<CommittedChangeList> changes = []
			Date date = fromDate
			Date endDate = (toDate == null ? (fromDate - 10.years) : toDate)

			new Iterator<CommittedChangeList>() {
				@Override boolean hasNext() {
					!changes.empty || date.after(endDate)
				}

				@Override CommittedChangeList next() {
					if (!changes.empty) return changes.remove(0)

					measure("git request time") {
						while (changes.empty && date.after(endDate)) {
							use(TimeCategory) {
								changes = requestChangeListsFor(project, date - 1.month, date)
								date = date - 1.month
							}
						}
					}

					changes.empty ? null : changes.remove(0)
				}

				@Override void remove() {
					throw new UnsupportedOperationException()
				}
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
		def handler = new Consumer<GitSimpleHandler>() {
			@Override void consume(GitSimpleHandler h) {
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
		GitUtil.getLocalCommittedChanges(project, root, handler, resultConsumer, (boolean) false)
		result
	}
}

class EventStorage {
	private final String name
	final String fileName

	EventStorage(String name) {
		this.name = name
		this.fileName = "${PathManager.pluginsPath}/delta-flora/${name}-events.csv"
	}

	def appendToEventsFile(Collection<ChangeEvent> changeEvents) {
		appendTo(fileName, toCsv(changeEvents))
	}

	Date getOldestEventTime() {
		def line = readLastLine(fileName)
		if (line == null) new Date()
		else new SimpleDateFormat(ChangeEvent.CSV_DATE_FORMAT).parse(line.split(",")[3])
	}

	Date getMostRecentEventTime() {
		def file = new File(fileName)
		if (!file.exists()) return new Date()
		file.withReader { reader -> new SimpleDateFormat(ChangeEvent.CSV_DATE_FORMAT).parse(reader.readLine().split(",")[3]) }
	}

	private static String readLastLine(String fileName) {
		def file = new File(fileName)
		if (!file.exists()) return null

		def randomAccess = new RandomAccessFile(file, "r")
		try {

			for (long pos = file.length() - 2; pos >= 0; pos--) {
				if (pos % 1000 == 0) {
					log(pos)
				}
				randomAccess.seek(pos)
				if (randomAccess.read() == '\n') {
					return randomAccess.readLine()
				}
			}
			randomAccess.seek(0)
			randomAccess.length() > 0 ? randomAccess.readLine() : null

		} finally {
			randomAccess.close()
		}
	}

	private static appendTo(String fileName, String text) {
		def file = new File(fileName)
		FileUtil.createParentDirs(file)
		file.append(text)
	}
}


class ChangeFinder {

	static List<ChangeEvent> changesEventsBetween(String beforeText, String afterText, CommitInfo commitInfo, Closure<PsiFile> parseAsPsi) {
		PsiFile psiBefore = measure("parsing time"){ parseAsPsi(beforeText) }
		PsiFile psiAfter = measure("parsing time"){ parseAsPsi(afterText) }

		def changedFragments = measure("diff time"){ new TextCompareProcessor(TRIM_SPACE).process(beforeText, afterText).findAll { it.type != null } }

		(List<ChangeEvent>) changedFragments.collectMany { LineFragment fragment ->
			measure("change events") {
				def offsetToLineNumber = { int offset -> fragment.type == DELETED ? toLineNumber(offset, beforeText) : toLineNumber(offset, afterText) }

				def revisionWithCode = (fragment.type == DELETED ? psiBefore : psiAfter)
				def range = (fragment.type == DELETED ? fragment.getRange(SIDE1) : fragment.getRange(SIDE2))

				List<ChangeEvent> changeEvents = []
				def addChangeEvent = { PsiNamedElement psiElement, int fromOffset, int toOffset ->
					def partialChangeEvent = new PartialChangeEvent(
							fullNameOf(psiElement),
							containingFileName(psiElement),
							diffTypeOf(fragment),
							offsetToLineNumber(fromOffset),
							offsetToLineNumber(toOffset),
							fromOffset,
							toOffset
					)
					def changeEvent = new ChangeEvent(
							partialChangeEvent,
							commitInfo
					)
					changeEvents << changeEvent
				}

				PsiNamedElement prevPsiElement = null
				int fromOffset = range.startOffset
				for (int offset = range.startOffset; offset < range.endOffset; offset++) {
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
		}
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
}

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
final class ChangeEvent {
	static final String CSV_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z"

	@Delegate PartialChangeEvent partialChangeEvent
	@Delegate CommitInfo commitInfo

	static String toCsv(Collection<ChangeEvent> changeEvents) {
		changeEvents.collect{it.toCsv()}.join("\n") + "\n"
	}

	String toCsv() {
		def commitMessageEscaped = '"' + commitMessage.replaceAll("\"", "\\\"") + '"'
		[elementName, revision, author, format(revisionDate), fileName,
				changeType, fromLine, toLine, fromOffset, toOffset, commitMessageEscaped].join(",")
	}

	private static String format(Date date) {
		new SimpleDateFormat(CSV_DATE_FORMAT).format(date)
	}
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
final class PartialChangeEvent {
	String elementName
	String fileName
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
		log(id + ": " + time)
		result
	}

	static record(String id, long duration) {
		durations[id] += duration
		log(id + ": " + duration)
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
			ChangeFinder.changesEventsBetween(beforeText, afterText, commitInfo, parseAsPsi)
		}
	}
}