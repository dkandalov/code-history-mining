package history.events

import groovy.transform.Immutable

@Immutable
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
