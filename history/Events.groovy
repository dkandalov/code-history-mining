package history

class Events {
	@SuppressWarnings("GroovyUnusedDeclaration")
	@groovy.transform.Immutable
	static class ChangeEvent {
		@Delegate CommitInfo commitInfo
		@Delegate FileChangeInfo fileChangeInfo
		@Delegate ElementChangeInfo partialChangeEvent
	}

	@SuppressWarnings("GroovyUnusedDeclaration")
	@groovy.transform.Immutable
	static class CommitInfo {
		String revision
		String author
		Date revisionDate
		String commitMessage
	}

	@SuppressWarnings("GroovyUnusedDeclaration")
	@groovy.transform.Immutable
	static class FileChangeInfo {
		String fileName
		String fileChangeType
		String packageBefore
		String packageAfter
		int linesInFileBefore
		int linesInFileAfter
	}

	@SuppressWarnings("GroovyUnusedDeclaration")
	@groovy.transform.Immutable
	static class ElementChangeInfo {
		static ElementChangeInfo EMPTY = new ElementChangeInfo("", "", 0, 0, 0, 0)

		String elementName
		String changeType
		int fromLine      // TODO use instead linesBefore/After
		int toLine
		int fromOffset
		int toOffset
	}

}
