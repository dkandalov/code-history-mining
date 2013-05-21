package history.unused

import com.intellij.openapi.diff.impl.fragments.LineFragment
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.psi.*
import history.CommitFilesMunger
import history.events.ChangeEvent
import history.events.ElementChangeInfo
import history.util.Measure

import static com.intellij.openapi.diff.impl.ComparisonPolicy.TRIM_SPACE
import static com.intellij.openapi.diff.impl.highlighting.FragmentSide.SIDE1
import static com.intellij.openapi.diff.impl.highlighting.FragmentSide.SIDE2
import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
import static history.MungingUtil.*
import static intellijeval.PluginUtil.runReadAction

class CommitMethodsMunger {
	private final Project project

	CommitMethodsMunger(Project project) {
		this.project = project
	}

	Collection<ChangeEvent> mungeCommit(CommittedChangeList commit) {
		try {
			def commitInfo = commitInfoOf(commit)
			commit.changes.collectMany { Change change ->
				def fileChangeInfo = CommitFilesMunger.fileChangeInfoOf(change, project)
				withDefault([null], elementChangesOf(change, project)).collect{
					new ChangeEvent(commitInfo, fileChangeInfo, it)
				}
			} as Collection<ChangeEvent>
		} catch (ProcessCanceledException ignore) {
			[]
		}
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
