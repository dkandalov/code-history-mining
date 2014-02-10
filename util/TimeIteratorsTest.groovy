package util

import org.junit.Test

import static util.DateTimeUtil.date
import static util.DateTimeUtil.dateTime

class TimeIteratorsTest {
	@Test void "should iterate from present to past in intervals"() {
		def fromDate = date("01/01/1970")
		def toDate = date("11/01/1970")
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
		def toDate = date("11/01/1970")
		def stepSizeInDays = 7

		def iterator = new PastToPresentIterator(fromDate, toDate, stepSizeInDays)

		assert iterator.hasNext()
		assert iterator.next() == [from: date("01/01/1970"), to: date("08/01/1970")]
		assert iterator.hasNext()
		assert iterator.next() == [from: date("08/01/1970"), to: date("11/01/1970")]
		assert !iterator.hasNext()
	}

	@Test void "should work with less than a day interval"() {
		def fromDate = dateTime("00:10 01/01/1970")
		def toDate = dateTime("00:30 01/01/1970")
		def stepSizeInDays = 2

		def iterator = new PresentToPastIterator(fromDate, toDate, stepSizeInDays)

		assert iterator.hasNext()
		assert iterator.next() == [from: fromDate, to: toDate]
		assert !iterator.hasNext()
	}
}
