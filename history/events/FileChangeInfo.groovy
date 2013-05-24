package history.events

import groovy.transform.Immutable

@Immutable
class FileChangeInfo {
	static final ChangeStats NA = new ChangeStats(-1, -1, -1, -1, -1)

	String fileName
	String fileChangeType
	String packageBefore
	String packageAfter
	ChangeStats lines
	ChangeStats chars


	@Override String toString() {
		"FileChangeInfo(\"$fileName\",\"$fileChangeType\",\"$packageBefore\",\"$packageAfter\", $lines, $chars)"
	}

	@Immutable
	static class ChangeStats {
		int before
		int after
		int added
		int modified
		int removed

		@Override String toString() {
			"ChangeStats($before, $after, $added, $modified, $removed)"
		}
	}
}
