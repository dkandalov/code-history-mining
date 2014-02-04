package analysis
import org.junit.Test

import static org.hamcrest.CoreMatchers.not
import static org.junit.Assert.assertThat
import static org.junit.matchers.JUnitMatchers.containsString

class CombiningVisualizationTest {
	@Test void "includes only one title but several tags and scripts"() {
		def context = new Visualization.Context([], "AProject")
		def html = Visualization.all.generate(context)

		assertThat(html, containsString("<title>AProject code history</title>"))
		assertThat(html, not(containsString("<title>Change size chart</title>")))
		assertThat(html, not(containsString("<title>Amount of committers</title>")))

		// this must be <span> tag because if it's <p> when saving page
		// chrome pushes child spans outside of <p>, this results in redundant ui elements when saved file opened again
		assertThat(html, containsString("<span id=\"change-size-chart\"></span>"))
		assertThat(html, containsString("<span id=\"amount-of-committers-chart\"></span>"))

		assertThat(html, containsString("createChart(\"change-size-chart\", rawData, projectName);"))
		assertThat(html, containsString("createChart(\"amount-of-committers-chart\", rawData, projectName);"))
	}
}
