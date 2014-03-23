package analysis
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

	static changeSizeChart = new Visualization("Change Size Chart", { Context context ->
		def json = changeSize_Chart(context.events, context.checkIfCancelled)
		changeSizeChartTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static amountOfCommittersChart = new Visualization("Amount Of Committers Chart", { Context context ->
		def json = amountOfCommitters_Chart(context.events, context.checkIfCancelled)
		amountOfCommittersChartTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static amountOfFilesInCommitChart = new Visualization("Amount Of Files In Commit Chart", { Context context ->
		def json = averageAmountOfFilesInCommit_Chart(context.events, context.checkIfCancelled)
		amountOfFilesInCommitChartTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static amountOfChangingFilesChart = new Visualization("Amount Of Changing Files Chart", { Context context ->
		def json = amountOfChangingFiles_Chart(context.events, context.checkIfCancelled)
		amountOfChangingFilesChartTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static changeSizeByFileTypeChart = new Visualization("Change Size By File Type Chart", { Context context ->
		def json = changeSizeByFileType_Chart(context.events, context.checkIfCancelled)
		changeSizeByFileTypeChartTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static filesInTheSameCommitGraph = new Visualization("Files In The Same Commit Graph", { Context context ->
		def json = filesInTheSameCommit_Graph(context.events, context.checkIfCancelled)
		filesInTheSameCommitGraphTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static committersChangingSameFilesGraph = new Visualization("Committers Changing Same Files Graph", { Context context ->
		def json = authorChangingSameFiles_Graph(context.events, context.checkIfCancelled)
		committersChangingSameFilesGraphTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static amountOfCommitsTreemap = new Visualization("Amount Of Commits Treemap", { Context context ->
		def json = TreeMapView.amountOfChangeInFolders_TreeMap(context.events, context.checkIfCancelled)
		amountOfCommitsTreemapTemplate.fillData(json).fillProjectName(context.projectName).text
		// TODO try sunburst layout? (http://bl.ocks.org/mbostock/4063423)
	})

	static commitTimePunchcard = new Visualization("Commit Time Punchcard", { Context context ->
		def json = commitsByDayOfWeekAndTime_PunchCard(context.events, context.checkIfCancelled)
		commitTimePunchcardTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static timeBetweenCommitsHistogram = new Visualization("Time Between Commits Histogram", { Context context ->
		def json = timeBetweenCommits_Histogram(context.events, context.checkIfCancelled)
		timeBetweenCommitsHistogramTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static commitMessageWordCloud = new Visualization("Commit Messages Word Cloud", { Context context ->
		def json = commitComments_WordCloud(context.events, context.checkIfCancelled)
		commitMessageWordCloudTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static all = createAllVisualizations(allVisualizationsTemplate)

	static commitLogAsGraph = new Visualization("Latest Commits As Graph", { Context context ->
		def json = commitLog_Graph(context.events, context.checkIfCancelled)
		commitLogAsGraphTemplate.fillData(json).fillProjectName(context.projectName).text
	})

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
