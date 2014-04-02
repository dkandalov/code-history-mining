package vcsaccess.implementation
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ContentRevision
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware
import events.CommitInfo

class CommitMungingUtil {
	static CommitInfo commitInfoOf(CommittedChangeList commit) {
		new CommitInfo(
				revisionNumberOf(commit),
				removeEmailFrom(commit.committerName),
				commit.commitDate,
				commit.comment.trim()
		)
	}

	static def nonEmptyRevisionOf(Change change) {
		change.afterRevision == null ? change.beforeRevision : change.afterRevision
	}

	static def contentOf(Change change) {
		def beforeText = withDefault("", change.beforeRevision?.content)
		def afterText = withDefault("", change.afterRevision?.content)
		[beforeText, afterText]
	}

	private static String revisionNumberOf(CommittedChangeList commit) {
		if (commit instanceof VcsRevisionNumberAware) {
			commit.revisionNumber.asString()
		} else {
			commit.number.toString()
		}
	}

	static String packageNameOf(ContentRevision contentRevision, String commonAncestorPath) {
		def path = contentRevision?.file?.parentPath?.path
		if (path == null || path == "") return ""

		// was observed in svn that some commits contain changes to file which are not part of project
		def isPartOfProject = path.contains(commonAncestorPath)
		isPartOfProject ? path.replace(commonAncestorPath, "") : null
	}

	private static String removeEmailFrom(String committerName) {
		committerName.replaceAll(/\s+<.+@.+>/, "").trim()
	}

	static <T> T withDefault(T defaultValue, T value) { value == null ? defaultValue : value }
}
