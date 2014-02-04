package analysis
import org.junit.Test

import static org.hamcrest.CoreMatchers.not
import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.containsString

class VisualizationTest {
	@Test void "combining all visualizations"() {
		def context = new Visualization.Context([], "AProject")
		def html = Visualization.all.generate(context)

		assertThat(html, containsString("<title>AProject code history</title>"))
		assertThat(html, not(containsString("<title>Change size chart</title>")))
		assertThat(html, not(containsString("<title>Amount of committers</title>")))

		assertThat(html, containsString("createChart(\"change-size-chart\", rawData, projectName);"))
		assertThat(html, containsString("createChart(\"amount-of-committers-chart\", rawData, projectName);"))
	}
}
