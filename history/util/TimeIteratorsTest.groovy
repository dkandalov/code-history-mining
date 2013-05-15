package history.util

import groovy.time.TimeCategory
import org.junit.Test

import static history.util.DateTimeUtil.date

class TimeIteratorsTest {
	@Test void "should iterate from present to past in intervals"() {
		def fromDate = date("01/01/1970")
		def toDate = use(TimeCategory){ fromDate + 10.days }
		def stepSizeInDays = 7

		def iterator = new PresentToPastIterator(fromDate, toDate, stepSizeInDays)

		assert iterator.hasNext()
		assert iterator.next() == [from: date("04/01/1970"), to: date("11/01/1970")]
		assert iterator.hasNext()
		assert iterator.next() == [from: date("01/01/1970"), to: date("04/01/1970")]
		assert !iterator.hasNext()
	}

	@Test void "should iterate from past to present in intervals"() {
		def fromDate = date("01/01/1970")
		def toDate = use(TimeCategory){ fromDate + 10.days }
		def stepSizeInDays = 7

		def iterator = new PastToPresentIterator(fromDate, toDate, stepSizeInDays)

		assert iterator.hasNext()
		assert iterator.next() == [from: date("01/01/1970"), to: date("08/01/1970")]
		assert iterator.hasNext()
		assert iterator.next() == [from: date("08/01/1970"), to: date("11/01/1970")]
		assert !iterator.hasNext()
	}
}
