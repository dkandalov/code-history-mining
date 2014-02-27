package analysis.templates
import org.junit.Test

import static AllTemplates.*

class TemplatesModificationTest {
	private ArrayList<Template> allTemplates = [
			changeSizeChartTemplate, amountOfCommittersChartTemplate, amountOfFilesInCommitChartTemplate,
			changeSizeByFileTypeChartTemplate, filesInTheSameCommitGraphTemplate, committersChangingSameFilesGraphTemplate,
			committersChangingSameFilesGraphTemplate, amountOfCommitsTreemapTemplate,
			commitTimePunchcardTemplate, timeBetweenCommitsHistogramTemplate, commitMessageWordCloudTemplate
	]

	@Test void "can remove template header"() {
		allTemplates.each{ template ->
			assertHeaderIsNoLongerPresentIn(template)
		}
	}
	@Test void "can change template width"() {
		allTemplates.each{ template ->
			assertWidthCanBeAdjustedIn(template)
		}
	}

	private static assertWidthCanBeAdjustedIn(Template template) {
		def newText = template.width(12345).text
		assert newText.contains("width = 12345,") || newText.contains("width: 12345")
	}

	private static assertHeaderIsNoLongerPresentIn(Template template) {
		assert template.text.contains("var headerSpan")
		assert template.text.contains("headerSpan.")
		assert !template.removeJsAddedHeader().text.contains("var headerSpan")
		assert !template.removeJsAddedHeader().text.contains("headerSpan.")
	}
}
