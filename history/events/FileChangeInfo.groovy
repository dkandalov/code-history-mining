package history.events

import groovy.transform.Immutable

@Immutable
class FileChangeInfo {
	static final int NA = -1

	String fileName
	String fileChangeType
	String packageBefore
	String packageAfter
	int linesInFileBefore
	int linesInFileAfter

	@Override String toString() {
		"FileChangeInfo(\"$fileName\",\"$fileChangeType\",\"$packageBefore\",\"$packageAfter\", " +
				"$linesInFileBefore, $linesInFileAfter)"
	}
}
