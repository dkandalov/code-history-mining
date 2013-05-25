package analysis

import org.junit.Test

import static analysis.Analysis.Util.pairs

class AnalysisTest {
	@Test void "should group elements into pairs"() {
		assert pairs([]) == []
		assert pairs([1]) == []
		assert pairs([1, 2]) == [[1, 2]]
		assert pairs([1, 2, 3, 4, 5]) == [[1, 2], [2, 3], [3, 4], [4, 5]]
		assert pairs([a: 1, b: 2, c: 3, d: 4].entrySet()) == [[a: 1, b: 2], [b: 2, c: 3], [c: 3, d: 4]]*.entrySet()*.toList()
	}
}
