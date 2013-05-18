package history.events

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class ElementChangeInfo {
	static ElementChangeInfo EMPTY = new ElementChangeInfo("", "", 0, 0, 0, 0)

	String elementName
	String changeType // TODO remove?
	int linesBefore
	int linesAfter
	int fromOffset // TODO chars before/after
	int toOffset
	// TODO complexity before/after

	@Override String toString() {
		"ElementChangeInfo(\"$elementName\", \"$changeType\", $linesBefore, $linesAfter, $fromOffset, $toOffset)"
	}
}
