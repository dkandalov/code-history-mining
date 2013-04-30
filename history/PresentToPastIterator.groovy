package history


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
		def newDate = chooseMostRecent(date - stepSizeInDays, fromDate)
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
