package util

class CollectionUtil {
	static <T> Collection<Collection<T>> collectWithHistory(Collection<T> collection, Closure shouldKeepElement, Closure callback) {
		def result = []
		def previousValues = new LinkedList()

		for (value in collection) {
			while (!previousValues.empty && !shouldKeepElement(previousValues.first(), value)) {
				previousValues = previousValues.tail()
			}

			result << callback(previousValues, value)

			if (shouldKeepElement(value, value)) previousValues << value
		}
		result
	}

	static <T> Collection<T> collectWithIndex(Collection<T> collection, Closure callback) {
		def result = new ArrayList<T>()
		int i = 0
		def iterator = collection.iterator()
		while (iterator.hasNext()) {
			T element =  iterator.next();
			result.add(callback(element, i))
			i++
		}
		result
	}

	static <T> Collection<Collection<T>> pairs(Collection<T> collection) {
		Collection<Collection<T>> result = collection.inject([]) { acc, value ->
			if (!acc.empty) acc.last() << value
			acc + [[value]]
		}
		if (!result.empty) result.remove(result.size() - 1)
		result
	}

	static int getSize(Collection collection) {
		collection.size()
	}
}
