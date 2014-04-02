package common.events

import groovy.transform.CompileStatic
import groovy.transform.Immutable

@CompileStatic
@Immutable
class ChangeStats {
	static final ChangeStats NA = new ChangeStats(-1, -1, -1, -1, -1)
	static final ChangeStats TOO_BIG_TO_DIFF = new ChangeStats(-2, -2, -2, -2, -2)

	int before
	int after
	int added
	int modified
	int removed

	@Override String toString() {
		if (this == NA) "NA"
		else if (this == TOO_BIG_TO_DIFF) "TOO_BIG_TO_DIFF"
		else "changeStats($before, $after, $added, $modified, $removed)"
	}
}
