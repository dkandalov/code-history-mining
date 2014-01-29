package events
import groovy.transform.CompileStatic

@CompileStatic
//@Immutable
class FileChangeEvent {
	@Delegate final CommitInfo commitInfo
	@Delegate final FileChangeInfo fileChangeInfo
	final Collection additionalAttributes

	FileChangeEvent(CommitInfo commitInfo, FileChangeInfo fileChangeInfo, Collection additionalAttributes = []) {
		this.commitInfo = commitInfo
		this.fileChangeInfo = fileChangeInfo
		this.additionalAttributes = additionalAttributes
	}

	@Override String toString() {
		if (additionalAttributes.empty)
			"fileChangeEvent($commitInfo, $fileChangeInfo)"
		else
			"fileChangeEvent($commitInfo, $fileChangeInfo, $additionalAttributes)"
	}

	@Override boolean equals(o) {
		if (this.is(o)) return true
		if (getClass() != o.class) return false

		FileChangeEvent that = (FileChangeEvent) o

		if (additionalAttributes != that.additionalAttributes) return false
		if (commitInfo != that.commitInfo) return false
		if (fileChangeInfo != that.fileChangeInfo) return false

		return true
	}

	@Override int hashCode() {
		int result
		result = commitInfo.hashCode()
		result = 31 * result + fileChangeInfo.hashCode()
		result = 31 * result + additionalAttributes.hashCode()
		return result
	}
}
