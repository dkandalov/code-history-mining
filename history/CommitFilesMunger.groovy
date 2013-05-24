package history
import com.intellij.openapi.diff.impl.highlighting.FragmentSide
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import history.events.ChangeStats
import history.events.FileChangeEvent
import history.events.FileChangeInfo

import static com.intellij.openapi.diff.impl.ComparisonPolicy.TRIM_SPACE
import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
import static com.intellij.openapi.util.io.FileUtil.toCanonicalPath
import static history.CommitMungingUtil.*
import static history.util.Measure.measure

class CommitFilesMunger {
	private final Project project
	private final boolean countLines

	CommitFilesMunger(Project project, boolean countLines = true) {
		this.countLines = countLines
		this.project = project
	}

	Collection<FileChangeEvent> mungeCommit(CommittedChangeList commit) {
		try {
			def commitInfo = commitInfoOf(commit)
			commit.changes.collect { Change change ->
				new FileChangeEvent(commitInfo, fileChangeInfoOf(change, project, countLines))
			}
		} catch (ProcessCanceledException ignore) {
			[]
		}
	}

	static FileChangeInfo fileChangeInfoOf(Change change, Project project, boolean readFileContent) {
		def nonEmptyRevision = nonEmptyRevisionOf(change)
		if (nonEmptyRevision.file.fileType.binary) readFileContent = false

		def lineChangesStats = FileChangeInfo.NA
		def charChangesStats = FileChangeInfo.NA
		if (readFileContent) {
			def (beforeText, afterText) = (readFileContent ? contentOf(change) : ["", ""])
			int linesBefore = beforeText.empty ? 0 : beforeText.split("\n").length
			int linesAfter = afterText.empty ? 0 : afterText.split("\n").length
			int charsBefore = beforeText.empty ? 0 : beforeText.length()
			int charsAfter = afterText.empty ? 0 : afterText.length()

			def fragmentsByType = new TextCompareProcessor(TRIM_SPACE).process(beforeText, afterText).groupBy{it.type}
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

			lineChangesStats = new ChangeStats(linesBefore, linesAfter, linesAdded, linesModified, linesRemoved)
			charChangesStats = new ChangeStats(charsBefore, charsAfter, charsAdded, charsModified, charsRemoved)
		}

		def projectPath = toCanonicalPath(project.basePath)
		def packageBefore = measure("VCS content time"){ withDefault("", change.beforeRevision?.file?.parentPath?.path).replace(projectPath, "") }
		def packageAfter = measure("VCS content time"){ withDefault("", change.afterRevision?.file?.parentPath?.path).replace(projectPath, "") }

		new FileChangeInfo(
				nonEmptyRevision.file.name,
				change.type.toString(),
				packageBefore,
				packageAfter == packageBefore ? "" : packageAfter,
				lineChangesStats,
				charChangesStats
		)
	}
}
