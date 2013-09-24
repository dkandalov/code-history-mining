package events

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
@Immutable
class ChangeStats {
	int before
	int after
	int added
	int modified
	int removed

	@Override String toString() {
		"ChangeStats($before, $after, $added, $modified, $removed)"
	}
}
