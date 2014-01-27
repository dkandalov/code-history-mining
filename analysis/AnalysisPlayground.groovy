package analysis
import events.EventStorage

import static http.HttpUtil.*
import static java.lang.System.getenv

class AnalysisPlayground {
	static void main(String[] args) {
		def projectName = "junit-2013"
		def filePath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea12/code-history-mining/${projectName}-file-events.csv"
		def events = new EventStorage(filePath).readAllEvents({}) { line, e -> println("Failed to parse line '${line}'") }

		fillTemplate("committers-changing-same-files-graph.html", projectName, Analysis.committersChangingFilesGraph(events))
	}

	static void fillTemplate(String template, String projectName, String jsValue) {
		def templateText = new File("templates/${template}").readLines().join("\n")
		def text = inlineJSLibraries(templateText) { fileName -> new File("templates/$fileName").readLines().join("\n") }
		text = fillDataPlaceholder(text, jsValue)
		text = fillProjectNamePlaceholder(text, "\"$projectName\"")
		new File("templates/${projectName}_${template}").write(text)
	}
}
