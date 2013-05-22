package history.unused

import history.events.CommitInfo
import history.events.ElementChangeInfo
import history.events.FileChangeInfo

@groovy.transform.Immutable
class MethodChangeEvent {
	@Delegate CommitInfo commitInfo
	@Delegate FileChangeInfo fileChangeInfo
	@Delegate ElementChangeInfo elementChangeInfo

	@Override String toString() {
		"MethodChangeEvent($commitInfo, $fileChangeInfo, $elementChangeInfo)"
	}
}
