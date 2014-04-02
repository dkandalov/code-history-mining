package analysis._private
import analysis.Context
import analysis.Visualization
import common.events.ChangeStats
import common.events.CommitInfo
import common.events.FileChangeEvent
import common.events.FileChangeInfo
import org.junit.Test

import static org.hamcrest.CoreMatchers.not
import static org.hamcrest.Matchers.containsString
import static org.junit.Assert.assertThat
import static common.langutil.DateTimeUtil.dateTime

class CombiningVisualizationTest {
	@Test void "includes only one title but several tags and scripts"() {
		def context = new Context([singleEvent], "AProject")
		def html = Visualization.all.generate(context)

		assertThat(html, containsString("<title>AProject code history</title>"))
		assertThat(html, not(containsString("<title>Change size chart</title>")))
		assertThat(html, not(containsString("<title>Amount of committers</title>")))

		// this must be <span> tag because if it's <p> when saving page
		// chrome pushes child spans outside of <p>, this results in redundant miner.ui elements when saved file opened again
		assertThat(html, containsString("<span id=\"change-size-chart\" class=\"bar-chart\"></span>"))
		assertThat(html, containsString("<span id=\"amount-of-committers-chart\" class=\"bar-chart\"></span>"))
	}

	private final singleEvent = new FileChangeEvent(
			new CommitInfo("b421d0ebd66701187c10c2b0c7f519dc435531ae", "Tim Perry", dateTime("19:37:57 01/04/2013"), "Added support for iterable datapoints"),
			new FileChangeInfo("", "AllMembersSupplier.java", "", "/src/main/java/org/junit/experimental/theories/internal", "MODIFICATION",
					new ChangeStats(178, 204, 23, 3, 0), new ChangeStats(6758, 7807, 878, 304, 0)
			)
	)
}
