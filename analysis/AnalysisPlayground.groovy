package analysis
import history.events.EventStorage

import static http.HttpUtil.fillDataPlaceholder
import static http.HttpUtil.fillProjectNamePlaceholder
import static http.HttpUtil.inlineJSLibraries
import static java.lang.System.getenv

class AnalysisPlayground {
	static void main(String[] args) {
//		def projectName = "delta-flora-for-intellij"
//		def projectName = "junit"
		def projectName = "fitnesse"
		def filePath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea12/code-history-mining/${projectName}-file-events.csv"
		def events = new EventStorage(filePath).readAllEvents { line, e -> println("Failed to parse line '${line}'") }

//		fillTemplate("calendar-view.html", projectName, Analysis.createJsonForCalendarView(events))
//		fillTemplate("changes-size-chart.html", projectName, Analysis.createJsonForBarChartView(events))
		fillTemplate("files-in-same-commit-graph.html", projectName, Analysis.createJson_FilesInTheSameCommit_Graph(events))
//		fillTemplate("wordcloud.html", projectName, Analysis.createJsonForCommitCommentWordCloud(events))
//		fillTemplate("treemap.html", projectName, Analysis.TreeMapView.createJsonForChangeSizeTreeMap(events))
//		fillTemplate("stacked_bars.html", projectName, Analysis.createJsonForCommitsStackBarsChart(events))
//		Analysis.createJson_AmountOfComitters_Chart(events)
//		Analysis.createJson_AverageAmountOfLinesChangedByDay_Chart(events)
//		Analysis.createJson_AverageAmountOfFilesInCommitByDay_Chart(events)
//		Analysis.createJson_CommitsWithAndWithoutTests_Chart(events)
//		fillTemplate("author-to-file-graph.html", projectName, Analysis.createJson_AuthorConnectionsThroughChangedFiles_Graph(events))
//				Analysis.createJson_CommitDayAndTime_PunchCard(events)

	}

	static void fillTemplate(String template, String projectName, String jsValue) {
		def templateText = new File("templates/${template}").readLines().join("\n")
		def text = inlineJSLibraries(templateText) { fileName -> new File("templates/$fileName").readLines().join("\n") }
		text = fillDataPlaceholder(text, jsValue)
		text = fillProjectNamePlaceholder(text, "\"$projectName\"")
		new File("templates/${projectName}_${template}").write(text)
	}

}
