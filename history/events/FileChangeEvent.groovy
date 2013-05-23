package history.events

import groovy.transform.Immutable


@Immutable
class FileChangeEvent {
	@Delegate CommitInfo commitInfo
	@Delegate FileChangeInfo fileChangeInfo

	@Override String toString() {
		"FileChangeEvent($commitInfo, $fileChangeInfo)"
	}
}
