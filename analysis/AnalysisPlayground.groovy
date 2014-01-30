package analysis
import events.EventStorage

import static http.HttpUtil.*
import static java.lang.System.getenv

class AnalysisPlayground {
	static void main(String[] args) {
		def projectName = "junit"
		def filePath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea12/code-history-mining/${projectName}-file-events-full.csv"
		def events = new EventStorage(filePath).readAllEvents({}) { line, e -> println("Failed to parse line '${line}'") }

		fillTemplate("changes-size-chart.html", projectName, Analysis.createJson_WiltComplexity_Chart(events))
	}

	static void fillTemplate(String template, String projectName, String jsValue) {
		def templateText = new File("templates/${template}").readLines().join("\n")
		def text = inlineJSLibraries(templateText) { fileName -> new File("templates/$fileName").readLines().join("\n") }
		text = fillDataPlaceholder(text, jsValue)
		text = fillProjectNamePlaceholder(text, "\"$projectName\"")
		new File("templates/${projectName}_${template}").write(text)
	}
}
