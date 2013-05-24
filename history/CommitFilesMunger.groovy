package history
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import history.events.FileChangeEvent
import history.events.FileChangeInfo

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

	static FileChangeInfo fileChangeInfoOf(Change change, Project project, boolean countLines) {
		def nonEmptyRevision = nonEmptyRevisionOf(change)
		if (nonEmptyRevision.file.fileType.binary) countLines = false

		int linesBefore = FileChangeInfo.NA
		int linesAfter = FileChangeInfo.NA
		if (countLines) {
			def (beforeText, afterText) = (countLines ? contentOf(change) : ["", ""])
			linesBefore = beforeText.empty ? 0 : beforeText.split("\n").length
			linesAfter = afterText.empty ? 0 : afterText.split("\n").length
		}

		def projectPath = toCanonicalPath(project.basePath)
		def packageBefore = measure("VCS content time"){ withDefault("", change.beforeRevision?.file?.parentPath?.path).replace(projectPath, "") }
		def packageAfter = measure("VCS content time"){ withDefault("", change.afterRevision?.file?.parentPath?.path).replace(projectPath, "") }

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
