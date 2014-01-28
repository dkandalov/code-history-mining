package events
import groovy.transform.CompileStatic

@CompileStatic
/*@Immutable*/ // effectively immutable; for some reason groovy compiler fails because of "Collection additional" field
class FileChangeEvent {
	@Delegate final CommitInfo commitInfo
	@Delegate final FileChangeInfo fileChangeInfo
	final Collection additional

	FileChangeEvent(CommitInfo commitInfo, FileChangeInfo fileChangeInfo, Collection additional = []) {
		this.commitInfo = commitInfo
		this.fileChangeInfo = fileChangeInfo
		this.additional = additional
	}

	@Override String toString() {
		"FileChangeEvent($commitInfo, $fileChangeInfo)"
	}

	@Override boolean equals(o) {
		if (this.is(o)) return true
		if (getClass() != o.class) return false

		FileChangeEvent that = (FileChangeEvent) o

		if (commitInfo != that.commitInfo) return false
		if (fileChangeInfo != that.fileChangeInfo) return false
		if (additional != that.additional) return false

		return true
	}

	@Override int hashCode() {
		int result
		result = commitInfo.hashCode()
		result = 31 * result + fileChangeInfo.hashCode()
		result = 31 * result + additional.hashCode()
		return result
	}
}
