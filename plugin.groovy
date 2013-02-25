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
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*

import java.text.SimpleDateFormat

import static com.intellij.openapi.diff.impl.ComparisonPolicy.TRIM_SPACE
import static com.intellij.openapi.diff.impl.highlighting.FragmentSide.SIDE1
import static com.intellij.openapi.diff.impl.highlighting.FragmentSide.SIDE2
import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
import static intellijeval.PluginUtil.*

if (isIdeStartup) return

//new TextCompareProcessorTestSuite(project).run()
//if (true) return

if (false) {
	def sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots.toList()
	def sourceRoot = sourceRoots.first()
	def vcsRoot = ProjectLevelVcsManager.getInstance(project).getVcsRootObjectFor(sourceRoot)
	if (vcsRoot == null) return

	def changesProvider = vcsRoot.vcs.committedChangesProvider
	def settings = changesProvider.createDefaultSettings()
	def location = changesProvider.getLocationFor(FilePathImpl.create(vcsRoot.path))
	List<CommittedChangeList> changeLists = changesProvider.getCommittedChanges(settings, location, changesProvider.unlimitedCountValue)
	show(changeLists.size())
	changeLists.each { changeList ->
		changeList.changes.each {
			show(it?.beforeRevision?.file?.name + "" + it?.afterRevision?.file?.name)
		}
	}
}


@groovy.transform.Immutable
final class ChangeEvent {
	@Delegate final PartialChangeEvent delegate
	final String revision
	final String author
	final Date revisionDate
	final String commitMessage
}

@groovy.transform.Immutable
final class PartialChangeEvent {
	final String elementName
	final String fileName
	final String changeType
	final int fromLine
	final int toLine
	final int fromOffset
	final int toOffset
}

def file = currentFileIn(project)
def (errorMessage, List<VcsFileRevision> revisions) = tryToGetHistoryFor(file, project)
if (errorMessage != null) {
	show(errorMessage)
	return
}
show("good to go")

def changeEvents = extractChangeEvents(file, revisions, project)
showInConsole(toCsv(changeEvents.take(10)), "output", project)

show("done")


static appendToEventsFile(List<List> changeEvents) {
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

static String toCsv(List<List> changeEvents) {
	changeEvents.collect{toCsvLine(it)}.join("\n") + "\n"
}
static String toCsvLine(List changeEvent) {
	def eventsAsString = changeEvent.collect {
		if (it instanceof Date) format((Date) it)
		else if (it instanceof TextDiffTypeEnum) format((TextDiffTypeEnum) it)
		else asString(it)
	}
	eventsAsString[eventsAsString.size() - 1] = '"' + eventsAsString.last().replaceAll("\"", "\\\"") + '"'
	eventsAsString.join(",")
}

static List<List> extractChangeEvents(VirtualFile file, List<VcsFileRevision> revisions, Project project) {
	def revisionPairs = (0..<revisions.size() - 1).collect { revisions[it, it + 1] }
	def compareProcessor = new TextCompareProcessor(TRIM_SPACE)
	def psiFileFactory = PsiFileFactory.getInstance(project)
	def parseAsPSI = { VcsFileRevision revision -> psiFileFactory.createFileFromText(file.name, file.fileType, new String(revision.content)) }

	revisionPairs.collectMany { VcsFileRevision before, VcsFileRevision after ->
		def beforeText = new String(before.content)
		def afterText = new String(after.content)
		def psiBefore = parseAsPSI(before)
		def psiAfter = parseAsPSI(after)

		def changedFragments = compareProcessor.process(beforeText, afterText).findAll{ it.type != null }
		changedFragments.collectMany { LineFragment fragment ->
			def offsetToLineNumber = { int offset -> fragment.type == DELETED ? toLineNumber(offset, beforeText) : toLineNumber(offset, afterText) }

			def revisionWithCode = (fragment.type == DELETED ? psiBefore : psiAfter)
			def range = (fragment.type == DELETED ? fragment.getRange(SIDE1) : fragment.getRange(SIDE2))

			def changeEvents = []
			def prevPsiElement = null
			for (int offset in range.startOffset..<range.endOffset) {
				PsiNamedElement psiElement = methodOrClassAt(offset, revisionWithCode)
				if (psiElement != prevPsiElement) {
					changeEvents << [
							fullNameOf(psiElement),
							after.revisionNumber.asString(),
							after.author,
							after.revisionDate,
							containingFileName(psiElement),
							changeTypeOf(fragment),
							offsetToLineNumber(offset),
							offsetToLineNumber(offset + 1),
							offset,
							offset + 1,
							after.commitMessage
					]
					prevPsiElement = psiElement
				} else {
					changeEvents.last()[7] = offsetToLineNumber(offset + 1)
					changeEvents.last()[9] = offset + 1
				}
			}
			changeEvents
		}
	}
}

static TextDiffTypeEnum changeTypeOf(LineFragment fragment) {
	// this is because fragment uses its child fragments type, which can be "INSERT/DELETED"
	// event though from line point of view it is "CHANGED"
	fragment.childrenIterator != null ? CHANGED : fragment.type
}

static String format(Date date) {
	new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").format(date)
}

static String format(TextDiffTypeEnum diffType) {
	switch (diffType) {
		case INSERT: return "added"
		case CHANGED: return "changed"
		case DELETED: return "deleted"
		case CONFLICT: return "conflict"
		case NONE: return "none"
		default: return "unknown"
	}
}

static int toLineNumber(int offset, String text) {
	int counter = 0
	for (int i = 0; i < offset; i++) {
		if (text.charAt(i) == '\n') counter++
	}
	counter
}

static String containingFileName(PsiElement psiElement) {
	if (psiElement == null) "null"
	else if (psiElement instanceof PsiFile) psiElement.name
	else (containingFileName(psiElement.parent))
}

static String fullNameOf(PsiElement psiElement) {
	if (psiElement == null) "null"
	else if (psiElement instanceof PsiFile) ""
	else if (psiElement instanceof PsiMethod || psiElement instanceof PsiClass) {
		def parentName = fullNameOf(psiElement.parent)
		parentName.empty ? psiElement.name : (parentName + "::" + psiElement.name)
	} else {
		fullNameOf(psiElement.parent)
	}
}

static PsiNamedElement methodOrClassAt(int offset, PsiFile psiFile) {
	parentMethodOrClassOf(psiFile.findElementAt(offset))
}

static PsiNamedElement parentMethodOrClassOf(PsiElement psiElement) {
	if (psiElement instanceof PsiMethod) psiElement as PsiNamedElement
	else if (psiElement instanceof PsiClass) psiElement as PsiNamedElement
	else if (psiElement instanceof PsiFile) psiElement as PsiNamedElement
	else parentMethodOrClassOf(psiElement.parent)
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
