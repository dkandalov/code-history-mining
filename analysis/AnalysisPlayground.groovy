package analysis
import events.EventStorage

import static http.HttpUtil.*
import static java.lang.System.getenv

class AnalysisPlayground {
	static void main(String[] args) {
		def projectName = "junit"
		def filePath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea12/code-history-mining/${projectName}-file-events-full.csv"
		def events = new EventStorage(filePath).readAllEvents({}) { line, e -> println("Failed to parse line '${line}'") }

		fillAndSaveTemplate("changes-size-chart.html", projectName, Analysis.createJson_ProjectSize_Chart(events))
	}

	static void fillAndSaveTemplate(String template, String projectName, String json) {
		def templateText = new File("templates/${template}").readLines().join("\n")
		def text = fillTemplate(templateText, projectName, json)// { fileName -> new File("templates/$fileName").readLines().join("\n") }
		new File("templates/${projectName}_${template}").write(text)
	}
}
