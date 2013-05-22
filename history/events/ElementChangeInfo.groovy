package history.events

@groovy.transform.Immutable
class ElementChangeInfo {
	String elementName
	int linesBefore
	int linesAfter
	int charsBefore
	int charsAfter

	@Override String toString() {
		"ElementChangeInfo(\"$elementName\", $linesBefore, $linesAfter, $charsBefore, $charsAfter)"
	}
}
