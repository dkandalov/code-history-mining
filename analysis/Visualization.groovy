package analysis

import analysis._private.Analysis
import analysis.templates.Template
import util.Measure

import static analysis._private.Analysis.*
import static analysis.templates.AllTemplates.*

class Visualization {
	final String name
	final Closure<String> generate
	final Measure measure = new Measure()

	Visualization(String name, Closure<String> generate) {
		this.name = name
		this.generate = { context ->
			measure.measure(name, { generate(context) })
		}
	}

	static changeSizeChart = new Visualization("Change Size Chart",
			combine(changeSizeChartTemplate, Analysis.&changeSize_Chart))

	static amountOfCommittersChart = new Visualization("Amount Of Committers Chart",
			combine(amountOfCommittersChartTemplate, Analysis.&amountOfCommitters_Chart))

	static amountOfFilesInCommitChart = new Visualization("Amount Of Files In Commit Chart",
			combine(amountOfFilesInCommitChartTemplate, Analysis.&averageAmountOfFilesInCommit_Chart))

	static amountOfChangingFilesChart = new Visualization("Amount Of Changing Files Chart",
			combine(amountOfChangingFilesChartTemplate, Analysis.&amountOfChangingFiles_Chart))

	static changeSizeByFileTypeChart = new Visualization("Change Size By File Type Chart",
			combine(changeSizeByFileTypeChartTemplate, Analysis.&changeSizeByFileType_Chart))

	static filesInTheSameCommitGraph = new Visualization("Files In The Same Commit Graph",
			combine(filesInTheSameCommitGraphTemplate, Analysis.&filesInTheSameCommit_Graph))

	static committersChangingSameFilesGraph = new Visualization("Committers Changing Same Files Graph",
			combine(committersChangingSameFilesGraphTemplate, Analysis.&authorChangingSameFiles_Graph))

	static amountOfCommitsTreemap = new Visualization("Amount Of Commits Treemap",
			combine(amountOfCommitsTreemapTemplate, TreeMapView.&amountOfChangeInFolders_TreeMap))

	static commitTimePunchcard = new Visualization("Commit Time Punchcard",
			combine(commitTimePunchcardTemplate, Analysis.&commitsByDayOfWeekAndTime_PunchCard))

	static timeBetweenCommitsHistogram = new Visualization("Time Between Commits Histogram",
			combine(timeBetweenCommitsHistogramTemplate, Analysis.&timeBetweenCommits_Histogram))

	static commitMessageWordCloud = new Visualization("Commit Messages Word Cloud",
			combine(commitMessageWordCloudTemplate, Analysis.&commitComments_WordCloud))

	static all = createAllVisualizations(allVisualizationsTemplate)

	static commitLogAsGraph = new Visualization("Latest Commits As Graph",
			combine(commitLogAsGraphTemplate, Analysis.&commitLog_Graph))


	private static Closure<String> combine(Template template, Closure<String> analysis) {
		{ Context context ->
			def json = analysis(context.events, context.checkIfCancelled)
			template.fillData(json).fillProjectName(context.projectName).text
		}
	}

	static Visualization createAllVisualizations(Template template) {
		new Visualization("All Visualizations", { Context context ->
			def templates = [
					changeSizeChartTemplate.fillData(changeSize_Chart(context.events, context.checkIfCancelled)),
					amountOfCommittersChartTemplate.fillData(amountOfCommitters_Chart(context.events, context.checkIfCancelled)),
					amountOfFilesInCommitChartTemplate.fillData(averageAmountOfFilesInCommit_Chart(context.events, context.checkIfCancelled)),
					amountOfChangingFilesChartTemplate.fillData(amountOfChangingFiles_Chart(context.events, context.checkIfCancelled)),
					changeSizeByFileTypeChartTemplate.fillData(changeSizeByFileType_Chart(context.events, context.checkIfCancelled)),
					filesInTheSameCommitGraphTemplate.fillData(filesInTheSameCommit_Graph(context.events, context.checkIfCancelled)),
					committersChangingSameFilesGraphTemplate.fillData(authorChangingSameFiles_Graph(context.events, context.checkIfCancelled)),
					amountOfCommitsTreemapTemplate.fillData(TreeMapView.amountOfChangeInFolders_TreeMap(context.events, context.checkIfCancelled)),
					commitTimePunchcardTemplate.fillData(commitsByDayOfWeekAndTime_PunchCard(context.events, context.checkIfCancelled)),
					timeBetweenCommitsHistogramTemplate.fillData(timeBetweenCommits_Histogram(context.events, context.checkIfCancelled)),
					commitMessageWordCloudTemplate.fillData(commitComments_WordCloud(context.events, context.checkIfCancelled))
			]

			template = template.fillProjectName(context.projectName.capitalize())
			templates.each{
				template = template.addBefore(
						"<!--style-insert-point-->",
						it.allTags("style").collect{
							it.replaceAll(/margin:.*?;/, '').replaceAll(/\swidth:.*?;/, '')
						}.join("\n")
				)
				template = template.addBefore(
						"<!--script-insert-point-->",
						it.removeJsAddedHeader().width(800).lastTag("script")
				)
				template = template.addBefore("<!--tag-insert-point-->", """
				<h4>${it.contentOfTag('title')}</h4>
        <span id="${it.mainTagId}"></span>
        <br/><br/>
			""")
			}
			template.removeJsAddedHeader().width(800).text
		})
	}

}
