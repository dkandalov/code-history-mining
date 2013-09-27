package analysis
import events.EventStorage
import analysis.Analysis

import static http.HttpUtil.fillDataPlaceholder
import static http.HttpUtil.fillProjectNamePlaceholder
import static http.HttpUtil.inlineJSLibraries
import static java.lang.System.getenv

class AnalysisPlayground {
	static void main(String[] args) {
//		def projectName = "code-history-mining"
		def projectName = "idea"
//		def projectName = "fitnesse"
//		def projectName = "scala"
		def filePath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea12/code-history-mining/${projectName}-file-events.csv"
		def events = new EventStorage(filePath).readAllEvents { line, e -> println("Failed to parse line '${line}'") }

//		new File("commentsAfter").write(events.collect{ it.commitMessage }.join("\n"))
		fillTemplate("calendar-view.html", projectName, Analysis.createJson_ChangeSize_Calendar(events))
		fillTemplate("changes-size-chart.html", projectName, Analysis.createJson_ChangeSize_Chart(events))
		fillTemplate("files-in-same-commit-graph.html", projectName, Analysis.createJson_FilesInTheSameCommit_Graph(events))
		fillTemplate("author-to-file-graph.html", projectName, Analysis.createJson_AuthorConnectionsThroughChangedFiles_Graph(events))
		fillTemplate("commit-time-punchcard.html", projectName, Analysis.createJson_CommitsByDayOfWeekAndTime_PunchCard(events))
		fillTemplate("wordcloud.html", projectName, Analysis.createJson_CommitComments_WordCloud(events))
		fillTemplate("treemap.html", projectName, Analysis.TreeMapView.createJson_AmountOfChangeInFolders_TreeMap(events))
		fillTemplate("time-between-commits-histogram.html", projectName, Analysis.createJson_TimeBetweenCommits_Histogram(events))

//		Analysis.createJson_AmountOfComitters_Chart(events)
//		Analysis.createJson_AverageAmountOfLinesChangedByDay_Chart(events)
//		Analysis.createJson_AverageAmountOfFilesInCommitByDay_Chart(events)
//		Analysis.createJson_CommitsWithAndWithoutTests_Chart(events)
	}

	static void fillTemplate(String template, String projectName, String jsValue) {
		def templateText = new File("templates/${template}").readLines().join("\n")
		def text = inlineJSLibraries(templateText) { fileName -> new File("templates/$fileName").readLines().join("\n") }
		text = fillDataPlaceholder(text, jsValue)
		text = fillProjectNamePlaceholder(text, "\"$projectName\"")
		new File("templates/${projectName}_${template}").write(text)
	}

}
