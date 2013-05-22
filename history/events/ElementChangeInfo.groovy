package history.events

@SuppressWarnings("GroovyUnusedDeclaration")
@groovy.transform.Immutable
class ElementChangeInfo {
	static ElementChangeInfo EMPTY = new ElementChangeInfo("", -1, -1, -1, -1)

	String elementName
	int linesBefore
	int linesAfter
	int charsBefore
	int charsAfter

	@Override String toString() {
		"ElementChangeInfo(\"$elementName\", $linesBefore, $linesAfter, $charsBefore, $charsAfter)"
	}
}
