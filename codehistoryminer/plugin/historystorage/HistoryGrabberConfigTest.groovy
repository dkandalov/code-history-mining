package codehistoryminer.plugin.historystorage
import org.junit.Test

import static codehistoryminer.core.common.langutil.DateTimeTestUtil.date
import static codehistoryminer.core.common.langutil.DateTimeTestUtil.time

class HistoryGrabberConfigTest {
	@Test void "convert state to/from json"() {
		def timeZone = TimeZone.default
		def config1 = new HistoryGrabberConfig(date("01/02/2013", timeZone), date("01/10/2013", timeZone), "outputPath1", false, false, time("11:22 01/10/2013"))
		def config2 = new HistoryGrabberConfig(date("01/02/2014", timeZone), date("01/10/2014", timeZone), "outputPath2", true, true, time("22:33 01/10/2014"))
		def state = ["project1": config1, "project2": config2]

		def stateAfter = HistoryGrabberConfig.Serializer.stateFromJson(
				HistoryGrabberConfig.Serializer.stateToJson(state))

		assert state == stateAfter
	}
}
