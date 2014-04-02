package common.langutil

import org.junit.Test

class CollectionUtilTest {
	@Test void "roll up map"() {
		Map.mixin(CollectionUtil)

		assert [:].rollup{it} == [:]
		assert [a: 1, b: 2].rollup{ it.size } == [a: 1, b: 2]

		assert [a: [1, 2], b: [3]].rollup{ it } == [a: [1, 2], b: [3]]
		assert [a: [1, 2], b: [3]].rollup{ it.size } == [a: 2, b: 1]
		assert [
				a: [a1: [1, 2], a2: [3]],
				b: [b1: [4], b2: [5, 6]]
		].rollup{ it.size } == [
				a: [a1: 2, a2: 1],
				b: [b1: 1, b2: 2]
		]
	}

	@Test void "collecting elements with index"() {
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

	@Test void "has size getter instead of method"() {
		Collection.mixin(CollectionUtil)
		assert [].size == 0
		assert [1, 2, 3].size == 3
	}
}
