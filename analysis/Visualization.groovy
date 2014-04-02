package analysis

import analysis.implementation.Analysis
import analysis.templates.Template
import common.langutil.Measure

import static analysis.implementation.Analysis.*
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

	static Visualization createAllVisualizations(Template mainTemplate, Closure prepareTemplate = {it}) {
		new Visualization("All Visualizations", { Context context ->
			def templateAndAnalysis = [
					[changeSizeChartTemplate, Analysis.&changeSize_Chart],
					[amountOfCommittersChartTemplate, Analysis.&amountOfCommitters_Chart],
					[amountOfFilesInCommitChartTemplate, Analysis.&averageAmountOfFilesInCommit_Chart],
					[amountOfChangingFilesChartTemplate, Analysis.&amountOfChangingFiles_Chart],
					[changeSizeByFileTypeChartTemplate, Analysis.&changeSizeByFileType_Chart],
					[filesInTheSameCommitGraphTemplate, Analysis.&filesInTheSameCommit_Graph],
					[committersChangingSameFilesGraphTemplate, Analysis.&authorChangingSameFiles_Graph],
					[amountOfCommitsTreemapTemplate, TreeMapView.&amountOfChangeInFolders_TreeMap],
					[commitTimePunchcardTemplate, Analysis.&commitsByDayOfWeekAndTime_PunchCard],
					[timeBetweenCommitsHistogramTemplate, Analysis.&timeBetweenCommits_Histogram],
					[commitMessageWordCloudTemplate, Analysis.&commitComments_WordCloud]
			]

			mainTemplate = mainTemplate.fillProjectName(context.projectName.capitalize())
			templateAndAnalysis.each{ Template template, Closure analysis ->
				template = prepareTemplate(template)
				template = template.fillData(analysis(context.events, context.checkIfCancelled))

				mainTemplate = mainTemplate.addBefore(
						"<!--style-insert-point-->",
						template.allTags("style").collect{
							it.replaceAll(/margin:.*?;/, '').replaceAll(/\swidth:.*?;/, '')
						}.join("\n")
				)
				mainTemplate = mainTemplate.addBefore(
						"<!--script-insert-point-->",
						template.removeJsAddedHeader().width(800).lastTag("script")
				)
				mainTemplate = mainTemplate.addBefore("<!--tag-insert-point-->", """
				<h4>${template.contentOfTag('title')}</h4>
        <span id="${template.mainTagId}"></span>
        <br/><br/>
			""")
			}
			mainTemplate.removeJsAddedHeader().width(800).text
		})
	}

}
