package history.events

import groovy.transform.Immutable


@Immutable
class CommitInfo {
	String revision
	String author
	Date revisionDate // TODO timezones (e.g. both GMT, BST in fitnesse file events)
	String commitMessage

	@Override String toString() {
		"CommitInfo(\"$revision\", \"$author\", \"$revisionDate\", \"$commitMessage\")"
	}
}
