package http
import org.junit.Test

import static http.AllTemplates.*

class AllTemplatesTest {
	private ArrayList<Template> allTemplates = [
			changeSizeChartTemplate, amountOfCommittersChartTemplate, amountOfFilesInCommitChartTemplate,
			filesInTheSameCommitGraphTemplate, committersChangingSameFilesGraphTemplate,
			committersChangingSameFilesGraphTemplate, amountOfCommitsTreemapTemplate
	]

	@Test void "removes header from templates"() {
		allTemplates.each{ template ->
			assertHeaderIsNoLongerPresentIn(template)
			assertWidthCanBeAdjustedIn(template)
		}
	}

	private static assertWidthCanBeAdjustedIn(Template template) {
		assert template.width(12345).text.contains("width = 12345,")
	}

	private static assertHeaderIsNoLongerPresentIn(Template template) {
		assert template.text.contains("var headerSpan")
		assert template.text.contains("headerSpan.")
		assert !template.removeJsAddedHeader().text.contains("var headerSpan")
		assert !template.removeJsAddedHeader().text.contains("headerSpan.")
	}
}
