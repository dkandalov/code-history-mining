package history.events

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class FileChangeInfo {
	String fileName
	String fileChangeType
	String packageBefore
	String packageAfter
	int linesInFileBefore
	int linesInFileAfter
}
