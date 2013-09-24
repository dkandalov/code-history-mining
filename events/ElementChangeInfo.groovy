package events

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
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
