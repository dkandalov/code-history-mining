package history.events


@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class CommitInfo {
	String revision
	String author
	Date revisionDate
	String commitMessage

	@Override String toString() {
		"CommitInfo(\"$revision\", \"$author\", \"$revisionDate\", \"$commitMessage\")"
	}
}
