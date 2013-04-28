import groovy.time.TimeCategory
import org.junit.Test
/**
 * User: dima
 * Date: 28/04/2013
 */

class PresentToPastIterator implements Iterator {
	final Date fromDate
	final Date toDate
	final int stepSizeInDays

	private Date date

	PresentToPastIterator(Date fromDate, Date toDate, int stepSizeInDays) {
		this.fromDate = fromDate
		this.toDate = toDate
		this.stepSizeInDays = stepSizeInDays

		date = toDate
	}

	@Override boolean hasNext() {
		date.after(fromDate)
	}

	@Override Object next() {
		def newDate = use(TimeCategory){ chooseMostRecent(date - stepSizeInDays, fromDate) }
		def result = [from: newDate, to: date]
		date = newDate
		result
	}

	private static Date chooseMostRecent(Date date1, Date date2) {
		date1.after(date2) ? date1 : date2
	}

	@Override void remove() {
		throw new UnsupportedOperationException()
	}
}
class PastToPresentIterator implements Iterator {
	final Date fromDate
	final Date toDate
	final int stepSizeInDays

	private Date date

	PastToPresentIterator(Date fromDate, Date toDate, int stepSizeInDays) {
		this.fromDate = fromDate
		this.toDate = toDate
		this.stepSizeInDays = stepSizeInDays

		date = fromDate
	}

	@Override boolean hasNext() {
		date.before(toDate)
	}

	@Override Object next() {
		def newDate = use(TimeCategory){ chooseOldest(date + stepSizeInDays, toDate) }
		def result = [from: date, to: newDate]
		date = newDate
		result
	}

	private static Date chooseOldest(Date date1, Date date2) {
		date1.before(date2) ? date1 : date2
	}

	@Override void remove() {
		throw new UnsupportedOperationException()
	}
}

class PresentToPastIteratorTest {
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