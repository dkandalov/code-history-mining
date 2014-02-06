package analysis._private
import org.junit.Test

import static analysis._private.Analysis.Util.*
import static util.DateTimeUtil.*

class AnalysisUtilTest {
	@Test def "collecting elements with index"() {
		Collection.mixin(Analysis.Util)
		assert ["a", "b", "c"].collectWithIndex{ value, i -> value + i} == ["a0", "b1", "c2"]
	}

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
		assert [].collectWithHistory(keepAllHistory){ history, value -> [history.clone(), value]} == []
		assert [1, 2, 3].collectWithHistory(keepAllHistory){ history, value -> [history.clone(), value]} == [
		    [[], 1],
		    [[1], 2],
		    [[1, 2], 3]
		]
	}

	@Test void "should collect elements while keeping history of elements which satisfy condition"() {
		Collection.mixin(Analysis.Util)

		def keepingValuesWithinRange = { value, currentValue -> currentValue - value < 3 }
		assert [1, 2, 3, 4].collectWithHistory(keepingValuesWithinRange){ history, value -> [history.clone(), value] } == [
		    [[], 1],
		    [[1], 2],
		    [[1, 2], 3],
		    [[2, 3], 4]
		]
	}

	@Test void "should floor time to day/week/month"() {
		assert floorToDay(exactDateTime("15:42:16 03/10/2013"), utc) == date("03/10/2013")

		assert floorToWeek(exactDateTime("15:42:16 03/10/2013"), utc) == date("30/09/2013")
		assert floorToWeek(exactDateTime("15:42:16 30/09/2013"), utc) == date("30/09/2013")
		assert floorToWeek(exactDateTime("15:42:16 30/01/2012"), utc) == date("30/01/2012")
		assert floorToWeek(exactDateTime("15:42:16 31/01/2012"), utc) == date("30/01/2012")

		assert floorToMonth(exactDateTime("15:42:16 03/10/2013"), utc) == date("01/10/2013")
	}
}
