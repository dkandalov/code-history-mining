package analysis._private
import org.junit.Test

import static analysis._private.Analysis.Util.*
import static util.DateTimeUtil.*

class AnalysisUtilTest {
	@Test void "should floor time to day/week/month"() {
		assert floorToDay(exactDateTime("15:42:16 03/10/2013"), utc) == date("03/10/2013")

		assert floorToWeek(exactDateTime("15:42:16 03/10/2013"), utc) == date("30/09/2013")
		assert floorToWeek(exactDateTime("15:42:16 30/09/2013"), utc) == date("30/09/2013")
		assert floorToWeek(exactDateTime("15:42:16 30/01/2012"), utc) == date("30/01/2012")
		assert floorToWeek(exactDateTime("15:42:16 31/01/2012"), utc) == date("30/01/2012")

		assert floorToMonth(exactDateTime("15:42:16 03/10/2013"), utc) == date("01/10/2013")
	}
}
