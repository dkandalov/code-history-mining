package history.events


@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class ChangeEvent {
	@Delegate CommitInfo commitInfo
	@Delegate FileChangeInfo fileChangeInfo

	@Override String toString() {
		"ChangeEvent($commitInfo, $fileChangeInfo)"
	}
}
