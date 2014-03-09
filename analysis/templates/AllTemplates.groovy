package analysis.templates
import com.intellij.openapi.util.io.FileUtil

class AllTemplates {
	static changeSizeChartTemplate = template("changes-size-chart.html")
	static amountOfCommittersChartTemplate = template("amount-of-committers-chart.html")
	static amountOfFilesInCommitChartTemplate = template("amount-of-files-in-commit-chart.html")
	static amountOfChangingFilesChartTemplate = template("amount-of-changing-files-chart.html")
	static changeSizeByFileTypeChartTemplate = template("change-size-by-file-type-chart.html")
	static filesInTheSameCommitGraphTemplate = template("files-in-same-commit-graph.html")
	static committersChangingSameFilesGraphTemplate = template("committers-changing-same-files-graph.html")
	static amountOfCommitsTreemapTemplate = template("treemap.html")
	static commitTimePunchcardTemplate = template("commit-time-punchcard.html")
	static timeBetweenCommitsHistogramTemplate = template("time-between-commits-histogram.html")
	static commitMessageWordCloudTemplate = template("wordcloud.html")

	static allVisualizationsTemplate = template("all-visualizations.html")
	static commitLogAsGraphTemplate = template("latest-commits-as-graph.html")

	private static Template template(String fileName) {
		new Template(readFile(fileName)).inlineImports(AllTemplates.&readFile)
	}

	static String readFile(String fileName) {
		FileUtil.loadTextAndClose(AllTemplates.class.getResourceAsStream("/analysis/templates/html/$fileName"))
	}
}
