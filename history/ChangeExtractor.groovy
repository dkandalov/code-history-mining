package history

import com.intellij.openapi.diff.impl.ComparisonPolicy
import com.intellij.openapi.diff.impl.fragments.LineFragment
import com.intellij.openapi.diff.impl.highlighting.FragmentSide
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.diff.impl.util.TextDiffTypeEnum
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.psi.PsiAnonymousClass
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiNamedElement
import history.events.ChangeEvent
import history.events.CommitInfo
import history.events.ElementChangeInfo
import history.events.FileChangeInfo
import history.util.Measure
import intellijeval.PluginUtil



class ChangeExtractor {

	static Collection<ChangeEvent> fileChangeEventsFrom(CommittedChangeList changeList, Project project) {
		try {
			def commitInfo = commitInfoOf(changeList)
			changeList.changes.collect { Change change ->
				new ChangeEvent(commitInfo, fileChangeInfoOf(change, project, false), ElementChangeInfo.EMPTY)
			}
		} catch (ProcessCanceledException ignore) {
			[]
		}
	}

	static Collection<ChangeEvent> changeEventsFrom(CommittedChangeList changeList, Project project) {
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
		def (beforeText, afterText) = (countFileLines ? contentOf(change) : ["", ""])

		def packageBefore = Measure.measure("VCS content time"){ withDefault("", change.beforeRevision?.file?.parentPath?.path).replace(project.basePath, "") }
		def packageAfter = Measure.measure("VCS content time"){ withDefault("", change.afterRevision?.file?.parentPath?.path).replace(project.basePath, "") }

		new FileChangeInfo(
				nonEmptyRevision.file.name,
				change.type.toString(),
				packageBefore,
				packageAfter == packageBefore ? "" : packageAfter,
				beforeText.split("\n").length,
				afterText.split("\n").length
		)
	}

	private static Collection<ElementChangeInfo> elementChangesOf(Change change, Project project) {
		def nonEmptyRevision = nonEmptyRevisionOf(change)
		if (nonEmptyRevision.file.fileType.binary) return []
		def (beforeText, afterText) = contentOf(change)

		elementChangesBetween(beforeText, afterText) { String text ->
			PluginUtil.runReadAction {
				def fileFactory = PsiFileFactory.getInstance(project)
				fileFactory.createFileFromText(nonEmptyRevision.file.name, nonEmptyRevision.file.fileType, text)
			} as PsiFile
		}
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
		PsiFile psiBefore = Measure.measure("parsing time"){ psiParser(beforeText) }
		PsiFile psiAfter = Measure.measure("parsing time"){ psiParser(afterText) }

		def changedFragments = Measure.measure("diff time") { new TextCompareProcessor(ComparisonPolicy.TRIM_SPACE).process(beforeText, afterText).findAll { it.type != null } }

		changedFragments.collectMany { LineFragment fragment ->
			Measure.measure("change events time") {
				def offsetToLineNumber = { int offset -> fragment.type == TextDiffTypeEnum.DELETED ? toLineNumber(offset, beforeText) : toLineNumber(offset, afterText) }

				List<ElementChangeInfo> elementChanges = []
				def addChangeEvent = { PsiNamedElement psiElement, int fromOffset, int toOffset ->
					def elementChange = new ElementChangeInfo(
							fullNameOf(psiElement),
							diffTypeOf(fragment),
							offsetToLineNumber(fromOffset),
							offsetToLineNumber(toOffset),
							fromOffset,
							toOffset
					)
					elementChanges << elementChange
				}

				def revisionWithCode = (fragment.type == TextDiffTypeEnum.DELETED ? psiBefore : psiAfter)
				def range = (fragment.type == TextDiffTypeEnum.DELETED ? fragment.getRange(FragmentSide.SIDE1) : fragment.getRange(FragmentSide.SIDE2))

				PsiNamedElement prevPsiElement = null
				int fromOffset = range.startOffset
				for (int offset = range.startOffset; offset < range.endOffset; offset++) {
					// running read action on fine-grained level because this seems to improve UI responsiveness
					// even though it will make the whole processing slower
					PluginUtil.runReadAction {
						PsiNamedElement psiElement = methodOrClassAt(offset, revisionWithCode)
						if (psiElement != prevPsiElement) {
							if (prevPsiElement != null)
								addChangeEvent(prevPsiElement, fromOffset, offset)
							prevPsiElement = psiElement
							fromOffset = offset
						}
					}
				}
				PluginUtil.runReadAction {
					if (prevPsiElement != null)
						addChangeEvent(prevPsiElement, fromOffset, range.endOffset)
				}

				elementChanges
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
		def diffType = (fragment.childrenIterator != null ? TextDiffTypeEnum.CHANGED : fragment.type)

		switch (diffType) {
			case TextDiffTypeEnum.INSERT: return "added"
			case TextDiffTypeEnum.CHANGED: return "changed"
			case TextDiffTypeEnum.DELETED: return "deleted"
			case TextDiffTypeEnum.CONFLICT: return "other"
			case TextDiffTypeEnum.NONE: return "other"
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

	private static <T> T withDefault(T defaultValue, T value) { value == null ? defaultValue : value }
}
