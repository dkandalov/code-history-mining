package history.events


@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class FileChangeEvent {
	@Delegate CommitInfo commitInfo
	@Delegate FileChangeInfo fileChangeInfo

	@Override String toString() {
		"FileChangeEvent($commitInfo, $fileChangeInfo)"
	}
}
