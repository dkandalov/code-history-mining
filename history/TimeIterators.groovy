package history
import groovy.time.TimeCategory

class TimeIterators {
	static class PresentToPastIterator implements Iterator {
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

	static class PastToPresentIterator implements Iterator {
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

}

