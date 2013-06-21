package historyreader
import com.intellij.openapi.diff.impl.ComparisonPolicy
import com.intellij.openapi.diff.impl.highlighting.FragmentSide
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.util.diff.FilesTooBigForDiffException
import events.ChangeStats
import events.FileChangeEvent
import events.FileChangeInfo

import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
import static com.intellij.openapi.util.io.FileUtil.toCanonicalPath
import static com.intellij.openapi.vcs.changes.Change.Type.MODIFICATION
import static historyreader.CommitMungingUtil.*
import static util.Measure.measure

class CommitFilesMunger {
	private final Project project
	private final boolean countChangeSizeInLines

	CommitFilesMunger(Project project, boolean countChangeSizeInLines) {
		this.countChangeSizeInLines = countChangeSizeInLines
		this.project = project
	}

	Collection<FileChangeEvent> mungeCommit(CommittedChangeList commit) {
		try {
			def commitInfo = commitInfoOf(commit)
			commit.changes.collect { Change change ->
				new FileChangeEvent(commitInfo, fileChangeInfoOf(change, project, countChangeSizeInLines))
			}
		} catch (ProcessCanceledException ignore) {
			[]
		}
	}

	static FileChangeInfo fileChangeInfoOf(Change change, Project project, boolean readFileContent) {
		def nonEmptyRevision = nonEmptyRevisionOf(change)
		if (nonEmptyRevision.file.fileType.binary) readFileContent = false

		def (lineChangesStats, charChangesStats) = [FileChangeInfo.NA, FileChangeInfo.NA]
		if (readFileContent) {
			(lineChangesStats, charChangesStats) = readChangeStats(change)
		}

		def projectPath = toCanonicalPath(project.basePath)
		def fileNameBefore = withDefault("", change.beforeRevision?.file?.name)
		def fileName = withDefault("", change.afterRevision?.file?.name)
		def packageNameBefore = measure("VCS content time"){ withDefault("", change.beforeRevision?.file?.parentPath?.path).replace(projectPath, "") }
		def packageName = measure("VCS content time"){ withDefault("", change.afterRevision?.file?.parentPath?.path).replace(projectPath, "") }

		def optimizedFileNameBefore = (change.type == MODIFICATION ? "" : fileNameBefore)
		def optimizedPackageNameBefore = (change.type == MODIFICATION ? "" : packageNameBefore)

		new FileChangeInfo(
				fileName,
				optimizedFileNameBefore,
				packageName,
				optimizedPackageNameBefore,
				change.type.toString(),
				lineChangesStats,
				charChangesStats
		)
	}

	private static readChangeStats(Change change) {
		def (beforeText, afterText) = contentOf(change)
		int linesBefore = beforeText.empty ? 0 : beforeText.split("\n").length
		int linesAfter = afterText.empty ? 0 : afterText.split("\n").length
		int charsBefore = beforeText.empty ? 0 : beforeText.length()
		int charsAfter = afterText.empty ? 0 : afterText.length()

		def fragmentsByType = [:]
		try {
			fragmentsByType = new TextCompareProcessor(ComparisonPolicy.IGNORE_SPACE)
					.process(beforeText, afterText).groupBy{ it.type }
		} catch (FilesTooBigForDiffException ignored) {
			return [FileChangeInfo.TOO_BIG_TO_DIFF, FileChangeInfo.TOO_BIG_TO_DIFF]
		}
		if (!fragmentsByType.containsKey(INSERT)) fragmentsByType[INSERT] = []
		if (!fragmentsByType.containsKey(CHANGED)) fragmentsByType[CHANGED] = []
		if (!fragmentsByType.containsKey(DELETED)) fragmentsByType[DELETED] = []

		int linesAdded = fragmentsByType[INSERT].sum(0){it.endLine2 - it.startingLine2}
		int linesModified = fragmentsByType[CHANGED].sum(0){it.endLine1 - it.startingLine1}
		int linesRemoved = fragmentsByType[DELETED].sum(0){it.endLine1 - it.startingLine1}

		def charsInFragments = { lineFragments, fragmentSide -> lineFragments.sum(0){ it.getRange(fragmentSide).length } }
		int charsAdded = charsInFragments(fragmentsByType[INSERT], FragmentSide.SIDE2)
		int charsModified = charsInFragments(fragmentsByType[CHANGED], FragmentSide.SIDE1)
		int charsRemoved = charsInFragments(fragmentsByType[DELETED], FragmentSide.SIDE1)

		[new ChangeStats(linesBefore, linesAfter, linesAdded, linesModified, linesRemoved),
		new ChangeStats(charsBefore, charsAfter, charsAdded, charsModified, charsRemoved)]
	}
}
