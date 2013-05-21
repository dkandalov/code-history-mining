package history

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import history.events.ChangeEvent
import history.events.ElementChangeInfo
import history.events.FileChangeInfo
import history.util.Measure

import static com.intellij.openapi.util.io.FileUtil.toCanonicalPath
import static history.MungingUtil.*

class CommitFilesMunger {
	private final Project project

	CommitFilesMunger(Project project) {
		this.project = project
	}

	Collection<ChangeEvent> mungeCommit(CommittedChangeList commit) {
		try {
			def commitInfo = commitInfoOf(commit)
			commit.changes.collect { Change change ->
				new ChangeEvent(commitInfo, fileChangeInfoOf(change, project, false), ElementChangeInfo.EMPTY)
			}
		} catch (ProcessCanceledException ignore) {
			[]
		}
	}

	static FileChangeInfo fileChangeInfoOf(Change change, Project project, boolean countFileLines = true) {
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
}
