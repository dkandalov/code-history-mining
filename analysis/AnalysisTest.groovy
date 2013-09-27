package analysis
import org.junit.Test
import analysis.Analysis

class AnalysisTest {
	@Test void "should group elements into pairs"() {
		Collection.mixin(Analysis.Util)
		assert [].pairs() == []
		assert [1].pairs() == []
		assert [1, 2].pairs() == [[1, 2]]
		assert [1, 2, 3, 4, 5].pairs() == [[1, 2], [2, 3], [3, 4], [4, 5]]
		assert [a: 1, b: 2, c: 3, d: 4].entrySet().pairs() == [[a: 1, b: 2], [b: 2, c: 3], [c: 3, d: 4]]*.entrySet()*.toList()
	}

	@Test void "should collect elements while keeping all history of past elements"() {
		Collection.mixin(Analysis.Util)

		def keepAllHistory = { value, currentValue -> true }
		assert [].collectWithHistory(keepAllHistory){ prevValues, value -> [prevValues.clone(), value]} == []
		assert [1, 2, 3].collectWithHistory(keepAllHistory){ prevValues, value -> [prevValues.clone(), value]} == [
		    [[], 1],
		    [[1], 2],
		    [[1, 2], 3]
		]
	}

	@Test void "should collect elements while keeping history of elements which satisfy condition"() {
		Collection.mixin(Analysis.Util)

		def keepValuesWithinRange = { value, currentValue -> currentValue - value < 3 }
		assert [1, 2, 3, 4].collectWithHistory(keepValuesWithinRange){ prevValues, value -> [prevValues.clone(), value] } == [
		    [[], 1],
		    [[1], 2],
		    [[1, 2], 3],
		    [[2, 3], 4]
		]
	}
}
