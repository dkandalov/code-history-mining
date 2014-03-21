package analysis._private
import historystorage.EventStorage
import analysis.templates.AllTemplates
import analysis.templates.Template

import static java.lang.System.getenv

class AnalysisPlayground {
	static void main(String[] args) {
		def projectName = "code-history-mining"
		def filePath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea13/code-history-mining/${projectName}-file-events.csv"
		def events = new EventStorage(filePath).readAllEvents({}) { line, e -> println("Failed to parse line '${line}'") }

		fillAndSaveTemplate("amount-of-changing-files-chart.html", projectName, Analysis.amountOfChangingFiles_Chart(events))
	}

	static void fillAndSaveTemplate(String template, String projectName, String json) {
		def templateText = new File("analysis/templates/html//${template}").readLines().join("\n")
		def text = new Template(templateText)
				.inlineImports{ AllTemplates.readFile(it) }
				.fillProjectName(projectName)
				.fillData(json)
				.text
		new File("analysis/templates/${projectName}_${template}").write(text)
	}
}
