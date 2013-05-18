package history.events

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class ElementChangeInfo {
	static ElementChangeInfo EMPTY = new ElementChangeInfo("", "", -1, -1, -1, -1)

	String elementName
	String changeType // TODO remove?
	int linesBefore
	int linesAfter
	int charsBefore
	int charsAfter
	// TODO complexity before/after

	@Override String toString() {
		"ElementChangeInfo(\"$elementName\", \"$changeType\", $linesBefore, $linesAfter, $charsBefore, $charsAfter)"
	}
}
