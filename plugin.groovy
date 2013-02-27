import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diff.impl.fragments.LineFragment
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.diff.impl.util.TextDiffTypeEnum
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.unscramble.UnscrambleDialog

import java.text.SimpleDateFormat

import static ChangeEvent.toCsv
import static com.intellij.openapi.diff.impl.ComparisonPolicy.TRIM_SPACE
import static com.intellij.openapi.diff.impl.highlighting.FragmentSide.SIDE1
import static com.intellij.openapi.diff.impl.highlighting.FragmentSide.SIDE2
import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
import static intellijeval.PluginUtil.*

if (isIdeStartup) return

//new TextCompareProcessorTestSuite(project).run()
//if (true) return

changeEventsForAllCommitsIn(project)

static changeEventsForAllCommitsIn(Project project) {
	def sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots.toList()
	def sourceRoot = sourceRoots.first()
	def vcsRoot = ProjectLevelVcsManager.getInstance(project).getVcsRootObjectFor(sourceRoot)
	if (vcsRoot == null) return

	def changesProvider = vcsRoot.vcs.committedChangesProvider
	def settings = changesProvider.createDefaultSettings()
	def location = changesProvider.getLocationFor(FilePathImpl.create(vcsRoot.path))
	List<CommittedChangeList> changeLists = changesProvider.getCommittedChanges(settings, location, changesProvider.unlimitedCountValue)

	show(changeLists.size())

	try {
		def changeEvents = (List<ChangeEvent>) changeLists.reverse().collectMany { CommittedChangeList changeList ->
			changeList.changes.collectMany { Change change ->
				change.with {
					def beforeText = (beforeRevision == null ? "" : beforeRevision.content)
					def afterText = (afterRevision == null ? "" : afterRevision.content)
					def nonEmptyRevision = (afterRevision == null ? beforeRevision : afterRevision)
					def commitInfo = new CommitInfo(
							nonEmptyRevision.revisionNumber.asString(),
							changeList.committerName, changeList.commitDate, changeList.comment.trim())
					def parseAsPsi = { String text ->
						def fileFactory = PsiFileFactory.getInstance(project)
						fileFactory.createFileFromText(nonEmptyRevision.file.name, nonEmptyRevision.file.fileType, text)
					}

					ChangeFinder.changesEventsBetween(beforeText, afterText, commitInfo, parseAsPsi)
				}
			}
		}
		showInConsole(toCsv(changeEvents.take(100)), "output", project)
	} catch (Exception e) {
		def writer = new StringWriter()
		def message = e
		message.printStackTrace(new PrintWriter(writer))
		message = UnscrambleDialog.normalizeText(writer.buffer.toString())
		showInNewConsole(message, "NPE", project)
	}
}

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


static appendToEventsFile(List<ChangeEvent> changeEvents) {
	appendTo("${PathManager.pluginsPath}/delta-flora/events.csv", toCsv(changeEvents))
}

static appendToLogFile(message) {
	appendTo("${PathManager.pluginsPath}/delta-flora/my.log", message.toString())
}

static appendTo(String fileName, String text) {
	def file = new File(fileName)
	FileUtil.createParentDirs(file)
	file.append(text)
}

static List<ChangeEvent> extractChangeEvents(VirtualFile file, List<VcsFileRevision> revisions, Project project) {
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

class ChangeFinder {
	static List<ChangeEvent> changesEventsBetween(String beforeText, String afterText, CommitInfo commitInfo, parseAsPsi) {
		PsiFile psiBefore = parseAsPsi(beforeText)
		PsiFile psiAfter = parseAsPsi(afterText)

		def changedFragments = new TextCompareProcessor(TRIM_SPACE).process(beforeText, afterText).findAll { it.type != null }
		(List<ChangeEvent>) changedFragments.collectMany { LineFragment fragment ->
			def offsetToLineNumber = { int offset -> fragment.type == DELETED ? toLineNumber(offset, beforeText) : toLineNumber(offset, afterText) }

			def revisionWithCode = (fragment.type == DELETED ? psiBefore : psiAfter)
			def range = (fragment.type == DELETED ? fragment.getRange(SIDE1) : fragment.getRange(SIDE2))

			List<ChangeEvent> changeEvents = []
			def prevPsiElement = null
			for (int offset in range.startOffset..<range.endOffset) {
				PsiNamedElement psiElement = methodOrClassAt(offset, revisionWithCode)
				if (psiElement != prevPsiElement) {
					def partialChangeEvent = new PartialChangeEvent(
							fullNameOf(psiElement),
							containingFileName(psiElement),
							diffTypeOf(fragment),
							offsetToLineNumber(offset),
							offsetToLineNumber(offset + 1),
							offset,
							offset + 1
					)
					def changeEvent = new ChangeEvent(
							partialChangeEvent,
							commitInfo
					)
					changeEvents << changeEvent
					prevPsiElement = psiElement
				} else {
					changeEvents[-1] = changeEvents[-1].updated(offsetToLineNumber(offset + 1), offset + 1)
				}
			}
			changeEvents
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

	private static String diffTypeAsString(TextDiffTypeEnum diffType) {
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
		else if (psiElement instanceof PsiMethod || psiElement instanceof PsiClass) {
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
		if (psiElement instanceof PsiMethod) psiElement as PsiNamedElement
		else if (psiElement instanceof PsiClass) psiElement as PsiNamedElement
		else if (psiElement instanceof PsiFile) psiElement as PsiNamedElement
		else parentMethodOrClassOf(psiElement.parent)
	}
}

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
final class ChangeEvent {
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
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(date)
	}

	ChangeEvent updated(int updatedToLine, int updatedToOffset) {
		new ChangeEvent(
				new PartialChangeEvent(elementName, fileName, changeType, fromLine, updatedToLine, fromOffset, updatedToOffset),
				new CommitInfo(revision, author, revisionDate, commitMessage)
		)
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

static tryToGetHistoryFor(VirtualFile file, Project project) {
	if (file == null) return ["Virtual file was null"]

	AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file)
	if (activeVcs == null) return ["There is no history for '${file.name}'"]

	def historySession = activeVcs.vcsHistoryProvider.createSessionFor(new FilePathImpl(file))
	def revisions = historySession.revisionList.sort{ it.revisionDate }
	if (revisions.size() < 1) return ["There are no committed revisions for '${file.name}'"]

	def noErrors = null
	[noErrors, revisions]
}
