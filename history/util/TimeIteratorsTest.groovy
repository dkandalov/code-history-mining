package history.util

import groovy.time.TimeCategory
import org.junit.Test

class TimeIteratorsTest {
	@Test void "should iterate from present into the past in intervals"() {
		def fromDate = date(1970, 1, 1)
		def toDate = use(TimeCategory){ fromDate + 10.days }
		def stepSizeInDays = 7

		def iterator = new PresentToPastIterator(fromDate, toDate, stepSizeInDays)

		assert iterator.hasNext()
		assert iterator.next() == [from: date(1970, 1, 4), to: date(1970, 1, 11)]
		assert iterator.hasNext()
		assert iterator.next() == [from: date(1970, 1, 1), to: date(1970, 1, 4)]
		assert !iterator.hasNext()
	}

	@Test void "should iterate from past into present in intervals"() {
		def fromDate = date(1970, 1, 1)
		def toDate = use(TimeCategory){ fromDate + 10.days }
		def stepSizeInDays = 7

		def iterator = new PastToPresentIterator(fromDate, toDate, stepSizeInDays)

		assert iterator.hasNext()
		assert iterator.next() == [from: date(1970, 1, 1), to: date(1970, 1, 8)]
		assert iterator.hasNext()
		assert iterator.next() == [from: date(1970, 1, 8), to: date(1970, 1, 11)]
		assert !iterator.hasNext()
	}

	private static Date date(int year, int month, int day) {
		new Date(year - 1900, month, day)
	}
}
