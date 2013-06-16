package historyreader.unused

import events.CommitInfo
import events.ElementChangeInfo
import events.FileChangeInfo

@groovy.transform.Immutable
class MethodChangeEvent {
	@Delegate CommitInfo commitInfo
	@Delegate FileChangeInfo fileChangeInfo
	@Delegate ElementChangeInfo elementChangeInfo

	@Override String toString() {
		"MethodChangeEvent($commitInfo, $fileChangeInfo, $elementChangeInfo)"
	}
}
