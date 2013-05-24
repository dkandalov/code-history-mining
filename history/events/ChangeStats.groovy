package history.events

import groovy.transform.Immutable

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
