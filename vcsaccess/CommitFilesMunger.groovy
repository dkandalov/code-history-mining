package vcsaccess
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
import util.Measure

import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
import static com.intellij.openapi.vcs.changes.Change.Type.MODIFICATION
import static com.intellij.openapi.vfs.VfsUtil.getCommonAncestor
import static vcsaccess.ChangeEventsReader.vcsRootsIn
import static vcsaccess._private.CommitMungingUtil.*

class CommitFilesMunger {
	private final Project project
	private final boolean countChangeSizeInLines
	private final List<Closure> additionalAttributeMungers
	private final Measure measure
	private final String commonAncestorPath

	CommitFilesMunger(Project project, boolean countChangeSizeInLines, Measure measure = new Measure(), List<Closure> additionalAttributeMungers = []) {
		this.countChangeSizeInLines = countChangeSizeInLines
		this.project = project
		this.measure = measure
		this.additionalAttributeMungers = additionalAttributeMungers
		this.commonAncestorPath = withDefault("", getCommonAncestor(vcsRootsIn(project).collect{it.path})?.canonicalPath)
	}

	Collection<FileChangeEvent> mungeCommit(CommittedChangeList commit) {
		try {
			def commitInfo = commitInfoOf(commit)
			commit.changes.collect { Change change ->
				def fileChangeInfo = fileChangeInfoOf(change, commonAncestorPath, countChangeSizeInLines)
				if (fileChangeInfo == null) return null

				def context = [commit: commit, change: change, project: project]
				def additionalAttributes = additionalAttributeMungers.collect{ it.call(context) }
				new FileChangeEvent(commitInfo, fileChangeInfo, additionalAttributes)

			}.findAll{ it != null }
		} catch (ProcessCanceledException ignore) {
			[]
		}
	}

	private FileChangeInfo fileChangeInfoOf(Change change, String commonAncestorPath, boolean readFileContent) {
		def nonEmptyRevision = nonEmptyRevisionOf(change)
		if (nonEmptyRevision.file.fileType.binary) readFileContent = false

		def (lineChangesStats, charChangesStats) = [ChangeStats.NA, ChangeStats.NA]
		if (readFileContent) {
			(lineChangesStats, charChangesStats) = readChangeStats(change)
		}

		def fileNameBefore = withDefault("", change.beforeRevision?.file?.name)
		def fileName = withDefault("", change.afterRevision?.file?.name)
		def packageNameBefore = measure.measure("VCS content time"){ packageNameOf(change.beforeRevision, commonAncestorPath) }
		def packageName = measure.measure("VCS content time"){ packageNameOf(change.afterRevision, commonAncestorPath) }
		if (packageNameBefore == null || packageName == null) return null

		def optimizedFileNameBefore = (change.type == MODIFICATION ? "" : fileNameBefore)
		def optimizedPackageNameBefore = (change.type == MODIFICATION ? "" : packageNameBefore)

		new FileChangeInfo(
				optimizedFileNameBefore,
				fileName,
				optimizedPackageNameBefore,
				packageName,
				change.type.toString(),
				lineChangesStats,
				charChangesStats
		)
	}

	private readChangeStats(Change change) {
		def (beforeText, afterText) = measure.measure("VCS content time"){ contentOf(change) }
		int linesBefore = beforeText.empty ? 0 : beforeText.split("\n").length
		int linesAfter = afterText.empty ? 0 : afterText.split("\n").length
		int charsBefore = beforeText.empty ? 0 : beforeText.length()
		int charsAfter = afterText.empty ? 0 : afterText.length()

		def fragmentsByType = [:]
		try {
			fragmentsByType = new TextCompareProcessor(ComparisonPolicy.IGNORE_SPACE)
					.process(beforeText, afterText).groupBy{ it.type }
		} catch (FilesTooBigForDiffException ignored) {
			return [ChangeStats.TOO_BIG_TO_DIFF, ChangeStats.TOO_BIG_TO_DIFF]
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
