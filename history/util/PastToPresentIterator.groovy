package history.util

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
		def newDate = chooseOldest(date + stepSizeInDays, toDate)
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
