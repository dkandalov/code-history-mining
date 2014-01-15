package events

import groovy.transform.Immutable

@Immutable
class CommitInfo {
	String revision
	String author
	Date revisionDate
	String commitMessage

	@Override String toString() {
		"CommitInfo(\"$revision\", \"$author\", \"$revisionDate\", \"$commitMessage\")"
	}
}
