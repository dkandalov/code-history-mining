package util
import org.junit.Test

import static util.DateTimeUtil.*

class DateTimeUtilTest {
	@Test void "time interval should move to next date"() {
		assert days().next(date("03/10/2013"), utc) == date("04/10/2013")
		assert days().next(dateTime("15:42:16 03/10/2013"), utc) == date("04/10/2013")
		assert days(30).next(date("03/10/2013"), utc) == date("02/11/2013")
		assert days(30).next(dateTime("15:42:16 03/10/2013"), utc) == date("02/11/2013")

		assert weeks().next(date("03/10/2013"), utc) == date("07/10/2013")
		assert weeks().next(dateTime("15:42:16 03/10/2013"), utc) == date("07/10/2013")
		assert weeks(2).next(date("03/10/2013"), utc) == date("14/10/2013")
		assert weeks(2).next(dateTime("15:42:16 03/10/2013"), utc) == date("14/10/2013")

		assert months().next(date("03/10/2013"), utc) == date("01/11/2013")
		assert months(2).next(date("03/10/2013"), utc) == date("01/12/2013")
	}

	@Test void "time interval should floor a date"() {
		assert days().floor(dateTime("15:42:16 03/10/2013"), utc) == date("03/10/2013")
		assert days(30).floor(dateTime("15:42:16 03/10/2013"), utc) == date("03/10/2013")

		assert weeks().floor(dateTime("15:42:16 03/10/2013"), utc) == date("30/09/2013")
		assert weeks().floor(dateTime("15:42:16 30/09/2013"), utc) == date("30/09/2013")
		assert weeks().floor(dateTime("15:42:16 30/01/2012"), utc) == date("30/01/2012")
		assert weeks().floor(dateTime("15:42:16 31/01/2012"), utc) == date("30/01/2012")
		assert weeks(2).floor(dateTime("15:42:16 31/01/2012"), utc) == date("30/01/2012")

		assert months().floor(dateTime("15:42:16 03/10/2013"), utc) == date("01/10/2013")
		assert months(2).floor(dateTime("15:42:16 03/10/2013"), utc) == date("01/10/2013")
	}

	@Test void "should floor time to day/week/month"() {
		assert floorToDay(dateTime("15:42:16 03/10/2013"), utc) == date("03/10/2013")

		assert floorToWeek(dateTime("15:42:16 03/10/2013"), utc) == date("30/09/2013")
		assert floorToWeek(dateTime("15:42:16 30/09/2013"), utc) == date("30/09/2013")
		assert floorToWeek(dateTime("15:42:16 30/01/2012"), utc) == date("30/01/2012")
		assert floorToWeek(dateTime("15:42:16 31/01/2012"), utc) == date("30/01/2012")

		assert floorToMonth(dateTime("15:42:16 03/10/2013"), utc) == date("01/10/2013")
	}
}
