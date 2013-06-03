package history.events

import groovy.transform.Immutable

@Immutable
class FileChangeInfo {
	static final ChangeStats NA = new ChangeStats(-1, -1, -1, -1, -1)
	static final ChangeStats TOO_BIG_TO_DIFF = new ChangeStats(-2, -2, -2, -2, -2)

	String fileName
	String fileNameBefore
	String packageName
	String packageNameBefore
	String fileChangeType
	ChangeStats lines
	ChangeStats chars


	@Override String toString() {
		"FileChangeInfo(\"$fileName\",\"$fileNameBefore\",\"$packageName\",\"$packageNameBefore\",\"$fileChangeType\",$lines,$chars)"
	}
}
