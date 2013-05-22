package history.events


@groovy.transform.Immutable
class FileChangeEvent {
	@Delegate CommitInfo commitInfo
	@Delegate FileChangeInfo fileChangeInfo

	@Override String toString() {
		"FileChangeEvent($commitInfo, $fileChangeInfo)"
	}
}
