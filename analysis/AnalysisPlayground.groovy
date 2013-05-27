package analysis
import history.events.EventStorage

import static http.HttpUtil.fillDataPlaceholder
import static http.HttpUtil.inlineJSLibraries
import static java.lang.System.getenv

class AnalysisPlayground {
	static void main(String[] args) {
//		def projectName = "delta-flora-for-intellij"
		def projectName = "junit"
		def filePath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea12/code-history-mining/${projectName}-file-events.csv"
		def events = new EventStorage(filePath).readAllEvents { line, e -> println("Failed to parse line '${line}'") }

//		fillTemplate("calendar_view.html", projectName, Analysis.createJsonForCalendarView(events))
//		fillTemplate("changes_size_chart.html", projectName, Analysis.createJsonForBarChartView(events))
//		fillTemplate("cooccurrences-graph.html", projectName, Analysis.createJsonForCooccurrencesGraph(events))
//		fillTemplate("wordcloud.html", projectName, Analysis.createJsonForCommitCommentWordCloud(events))
//		fillTemplate("treemap.html", projectName, Analysis.TreeMapView.createJsonForChangeSizeTreeMap(events))
//		fillTemplate("stacked_bars.html", projectName, Analysis.createJsonForCommitsStackBarsChart(events))
//		Analysis.createJson_AmountOfComitters_Chart(events)
//		Analysis.createJson_AverageAmountOfLinesChangedByDay_Chart(events)
//		Analysis.createJson_AverageAmountOfFilesInCommitByDay_Chart(events)
		Analysis.createJson_CommitsWithAndWithoutTests_Chart(events)
	}

	static void fillTemplate(String template, String projectName, String jsValue) {
		def templateText = new File("templates/${template}").readLines().join("\n")
		def text = inlineJSLibraries(templateText) { fileName -> new File("templates/$fileName").readLines().join("\n") }
		text = fillDataPlaceholder(text, jsValue)
		new File("templates/${projectName}_${template}").write(text)
	}

}
