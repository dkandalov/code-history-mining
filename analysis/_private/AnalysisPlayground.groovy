package analysis._private
import historystorage.EventStorage
import analysis.templates.AllTemplates
import analysis.templates.Template

import static java.lang.System.getenv

class AnalysisPlayground {
	static void main(String[] args) {
		def projectName = "junit"
		def filePath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea12/code-history-mining/${projectName}-file-events-full.csv"
		def events = new EventStorage(filePath).readAllEvents({}) { line, e -> println("Failed to parse line '${line}'") }

		fillAndSaveTemplate("changes-size-chart.html", projectName, Analysis.projectSize_Chart(events))
	}

	static void fillAndSaveTemplate(String template, String projectName, String json) {
		def templateText = new File("templates/${template}").readLines().join("\n")
		def text = new Template(templateText)
				.inlineImports{ AllTemplates.readFile(it) }
				.fillProjectName(projectName)
				.fillData(json)
				.text
		new File("templates/${projectName}_${template}").write(text)
	}
}
