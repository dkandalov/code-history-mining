package events

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
@Immutable
class FileChangeEvent {
	@Delegate CommitInfo commitInfo
	@Delegate FileChangeInfo fileChangeInfo

	@Override String toString() {
		"FileChangeEvent($commitInfo, $fileChangeInfo)"
	}
}
