package history.events

/**
 * User: dima
 * Date: 01/05/2013
 */
@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class CommitInfo {
	String revision
	String author
	Date revisionDate
	String commitMessage
}
