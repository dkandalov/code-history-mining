package util

import org.junit.Test

class CollectionUtilTest {
	@Test def "collecting elements with index"() {
		Collection.mixin(CollectionUtil)
		assert ["a", "b", "c"].collectWithIndex{ value, i -> value + i} == ["a0", "b1", "c2"]
	}

	@Test void "should group elements into pairs"() {
		Collection.mixin(CollectionUtil)
		assert [].pairs() == []
		assert [1].pairs() == []
		assert [1, 2].pairs() == [[1, 2]]
		assert [1, 2, 3, 4, 5].pairs() == [[1, 2], [2, 3], [3, 4], [4, 5]]
		assert [a: 1, b: 2, c: 3, d: 4].entrySet().pairs() == [[a: 1, b: 2], [b: 2, c: 3], [c: 3, d: 4]]*.entrySet()*.toList()
	}

	@Test void "should collect elements while keeping all history of past elements"() {
		Collection.mixin(CollectionUtil)

		def keepAllHistory = { value, currentValue -> true }
		assert [].collectWithHistory(keepAllHistory){ history, value -> [history.clone(), value]} == []
		assert [1, 2, 3].collectWithHistory(keepAllHistory){ history, value -> [history.clone(), value]} == [
				[[], 1],
				[[1], 2],
				[[1, 2], 3]
		]
	}

	@Test void "should collect elements while keeping history of elements which satisfy condition"() {
		Collection.mixin(CollectionUtil)

		def keepingValuesWithinRange = { value, currentValue -> currentValue - value <= 2 }
		assert [1, 2, 3, 4].collectWithHistory(keepingValuesWithinRange){ history, value -> [history.clone(), value] } == [
				[[], 1],
				[[1], 2],
				[[1, 2], 3],
				[[2, 3], 4]
		]
	}

}
