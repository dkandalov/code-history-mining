package history.events

/**
 * User: dima
 * Date: 01/05/2013
 */
@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class ChangeEvent {
	@Delegate CommitInfo commitInfo
	@Delegate FileChangeInfo fileChangeInfo
	@Delegate ElementChangeInfo partialChangeEvent
}
