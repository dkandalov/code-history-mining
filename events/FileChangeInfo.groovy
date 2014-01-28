package events

import groovy.transform.Immutable

@Immutable
class FileChangeInfo {
	static final ChangeStats NA = new ChangeStats(-1, -1, -1, -1, -1)
	static final ChangeStats TOO_BIG_TO_DIFF = new ChangeStats(-2, -2, -2, -2, -2)

	String fileNameBefore
	String fileName
	String packageNameBefore
	String packageName
	String fileChangeType
	ChangeStats lines
	ChangeStats chars
	Collection additionalAttributes = []


	@Override String toString() {
		"FileChangeInfo(\"$fileNameBefore\",\"$fileName\",\"$packageNameBefore\",\"$packageName\",\"$fileChangeType\",$lines,$chars)"
	}
}
