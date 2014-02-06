package events

import groovy.transform.Immutable

@Immutable
class FileChangeInfo {

	String fileNameBefore
	String fileName
	String packageNameBefore
	String packageName
	String fileChangeType // see com.intellij.openapi.vcs.changes.Change.Type
	ChangeStats lines
	ChangeStats chars


	@Override String toString() {
		"fileChangeInfo(\"$fileNameBefore\", \"$fileName\", \"$packageNameBefore\", \"$packageName\", \"$fileChangeType\", $lines, $chars)"
	}
}
