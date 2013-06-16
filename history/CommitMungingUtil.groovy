package history

import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware
import events.CommitInfo
import util.Measure

class CommitMungingUtil {
	static CommitInfo commitInfoOf(CommittedChangeList commit) {
		new CommitInfo(
				revisionNumberOf(commit),
				removeEmailFrom(commit.committerName),
				commit.commitDate, commit.comment.trim()
		)
	}

	static def nonEmptyRevisionOf(Change change) {
		change.afterRevision == null ? change.beforeRevision : change.afterRevision
	}

	static def contentOf(Change change) {
		Measure.measure("VCS content time") {
			def beforeText = withDefault("", change.beforeRevision?.content)
			def afterText = withDefault("", change.afterRevision?.content)
			[beforeText, afterText]
		}
	}

	private static String revisionNumberOf(CommittedChangeList commit) {
		if (commit instanceof VcsRevisionNumberAware) {
			commit.revisionNumber.asString()
		} else {
			commit.number.toString()
		}
	}

	private static String removeEmailFrom(String committerName) {
		committerName.replaceAll(/\s+<.+@.+>/, "").trim()
	}

	static <T> T withDefault(T defaultValue, T value) { value == null ? defaultValue : value }
}
