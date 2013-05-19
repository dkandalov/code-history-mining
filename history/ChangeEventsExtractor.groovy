package history
import com.intellij.openapi.diff.impl.fragments.LineFragment
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware
import com.intellij.psi.*
import history.events.ChangeEvent
import history.events.CommitInfo
import history.events.ElementChangeInfo
import history.events.FileChangeInfo
import history.util.Measure

import static com.intellij.openapi.diff.impl.ComparisonPolicy.TRIM_SPACE
import static com.intellij.openapi.diff.impl.highlighting.FragmentSide.SIDE1
import static com.intellij.openapi.diff.impl.highlighting.FragmentSide.SIDE2
import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
import static com.intellij.openapi.util.io.FileUtil.toCanonicalPath
import static intellijeval.PluginUtil.runReadAction

class ChangeEventsExtractor {
	private final Project project

	ChangeEventsExtractor(Project project) {
		this.project = project
	}

	Collection<ChangeEvent> fileChangeEventsFrom(CommittedChangeList changeList) {
		try {
			def commitInfo = commitInfoOf(changeList)
			changeList.changes.collect { Change change ->
				new ChangeEvent(commitInfo, fileChangeInfoOf(change, project, false), ElementChangeInfo.EMPTY)
			}
		} catch (ProcessCanceledException ignore) {
			[]
		}
	}

	Collection<ChangeEvent> changeEventsFrom(CommittedChangeList changeList) {
		try {
			def commitInfo = commitInfoOf(changeList)
			changeList.changes.collectMany { Change change ->
				def fileChangeInfo = fileChangeInfoOf(change, project)
				withDefault([null], elementChangesOf(change, project)).collect{
					new ChangeEvent(commitInfo, fileChangeInfo, it)
				}
			} as Collection<ChangeEvent>
		} catch (ProcessCanceledException ignore) {
			[]
		}
	}

	private static CommitInfo commitInfoOf(CommittedChangeList changeList) {
		new CommitInfo(
			revisionNumberOf(changeList),
			removeEmailFrom(changeList.committerName),
			changeList.commitDate, changeList.comment.trim()
		)
	}

	private static FileChangeInfo fileChangeInfoOf(Change change, Project project, boolean countFileLines = true) {
		def nonEmptyRevision = nonEmptyRevisionOf(change)
		if (nonEmptyRevision.file.fileType.binary) countFileLines = false

		int linesBefore = FileChangeInfo.NA
		int linesAfter = FileChangeInfo.NA
		if (countFileLines) {
			def (beforeText, afterText) = (countFileLines ? contentOf(change) : ["", ""])
			linesBefore = beforeText.empty ? 0 : beforeText.split("\n").length
			linesAfter = afterText.empty ? 0 : afterText.split("\n").length
		}

		def projectPath = toCanonicalPath(project.basePath)
		def packageBefore = Measure.measure("VCS content time"){ withDefault("", change.beforeRevision?.file?.parentPath?.path).replace(projectPath, "") }
		def packageAfter = Measure.measure("VCS content time"){ withDefault("", change.afterRevision?.file?.parentPath?.path).replace(projectPath, "") }

		new FileChangeInfo(
				nonEmptyRevision.file.name,
				change.type.toString(),
				packageBefore,
				packageAfter == packageBefore ? "" : packageAfter,
				linesBefore,
				linesAfter
		)
	}

	private static Collection<ElementChangeInfo> elementChangesOf(Change change, Project project) {
		def nonEmptyRevision = nonEmptyRevisionOf(change)
		if (nonEmptyRevision.file.fileType.binary) return []
		def (beforeText, afterText) = contentOf(change)

		def psiParser = { String text ->
			runReadAction{
				def fileFactory = PsiFileFactory.getInstance(project)
				fileFactory.createFileFromText(nonEmptyRevision.file.name, nonEmptyRevision.file.fileType, text)
			} as PsiFile
		}
		elementChangesBetween(beforeText, afterText, psiParser)
	}

	private static def nonEmptyRevisionOf(Change change) {
		change.afterRevision == null ? change.beforeRevision : change.afterRevision
	}

	private static def contentOf(Change change) {
		Measure.measure("VCS content time") {
			def beforeText = withDefault("", change.beforeRevision?.content)
			def afterText = withDefault("", change.afterRevision?.content)
			[beforeText, afterText]
		}
	}

	static Collection<ElementChangeInfo> elementChangesBetween(String beforeText, String afterText, Closure<PsiFile> psiParser) {
		PsiFile fileBeforeChange = Measure.measure("parsing time"){ psiParser(beforeText) }
		PsiFile fileAfterChange = Measure.measure("parsing time"){ psiParser(afterText) }
		def changedFragments = Measure.measure("diff time") { new TextCompareProcessor(TRIM_SPACE).process(beforeText, afterText).findAll { it.type != null } }

		def psiElements = new HashSet()
		changedFragments.each{ fragment ->
			if (fragment.type == CHANGED) { // TODO not 100% sure that it will be always the same method in before/after code
				psiElements.addAll(elementsChangedBy(fragment, fileBeforeChange))
			} else if (fragment.type == DELETED) {
				psiElements.addAll(elementsChangedBy(fragment, fileBeforeChange))
			} else if (fragment.type == INSERT) {
				psiElements.addAll(elementsChangedBy(fragment, fileAfterChange))
			} else {
				throw new IllegalStateException()
			}
		}

		psiElements.collect{ PsiNamedElement element ->
			new ElementChangeInfo(
					fullNameOf(element),
					runReadAction{ sizeInLinesOf(element, fileBeforeChange) },
					runReadAction{ sizeInLinesOf(element, fileAfterChange) },
					runReadAction{ sizeInCharsOf(element, fileBeforeChange) },
					runReadAction{ sizeInCharsOf(element, fileAfterChange) }
			)
		}
	}

	private static Collection<PsiElement> elementsChangedBy(LineFragment fragment, PsiFile psiFile) {
		def range = (fragment.type == DELETED ? fragment.getRange(SIDE1) : fragment.getRange(SIDE2))

		def result = new HashSet()
		for (int offset = range.startOffset; offset < range.endOffset; offset++) {
			// running read action on fine-grained level because this seems to make UI more responsive
			// (even though it will make the whole processing slower)
			runReadAction {
				def element = methodOrClassAt(offset, psiFile)
				if (element != null) result << element
			}
		}
		result
	}

	private static int sizeInLinesOf(PsiNamedElement psiElement, PsiFile psiFile) {
		int result = 0
		psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
			@Override void visitElement(PsiElement element) {
				if (element.class == psiElement.class && element.name == psiElement.name) {
					result = element.text.empty ? 0 : element.text.split("\n").size()
				} else {
					super.visitElement(element)
				}
			}
		})
		result
	}

	private static int sizeInCharsOf(PsiNamedElement psiElement, PsiFile psiFile) {
		int result = 0
		psiFile.acceptChildren(new PsiRecursiveElementVisitor() {
			@Override void visitElement(PsiElement element) {
				if (element.class == psiElement.class && element.name == psiElement.name) {
					result = element.textLength
				} else {
					super.visitElement(element)
				}
			}
		})
		result
	}

	private static String revisionNumberOf(CommittedChangeList changeList) {
		if (changeList instanceof VcsRevisionNumberAware) {
			changeList.revisionNumber.asString()
		} else {
			changeList.number.toString()
		}
	}

	private static String removeEmailFrom(String committerName) {
		committerName.replaceAll(/\s+<.+@.+>/, "").trim()
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

	private static <T> T withDefault(T defaultValue, T value) { value == null ? defaultValue : value }
}
