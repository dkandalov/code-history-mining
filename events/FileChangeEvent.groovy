package events
import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
@Immutable
class FileChangeEvent {
	@Delegate final CommitInfo commitInfo
	@Delegate final FileChangeInfo fileChangeInfo

	@Override String toString() {
		"FileChangeEvent($commitInfo, $fileChangeInfo)"
	}
}
