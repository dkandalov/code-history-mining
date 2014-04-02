package analysis._private
import analysis.Context
import historystorage.EventStorage
import common.langutil.Measure

import static analysis.Visualization.*
import static java.lang.System.getenv

class AnalysisPlayground {
	static void main(String[] args) {
		def projectName = "scala"
		def fileName = "scala"
		def csvPath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea13/code-history-mining"
		def filePath = "${csvPath}/${fileName}-file-events.csv"

		def readEvents = { new EventStorage(filePath).readAllEvents({}){ line, e -> println("Failed to parse line '${line}'") } }
		def readEventsTime = new Measure()
		def events = readEventsTime.measure("Read all events", readEvents)
		readEventsTime.forEachDuration{println(it)}

		def context = new Context(events, projectName)
		[changeSizeChart,
		 amountOfFilesInCommitChart,
		 amountOfChangingFilesChart,
		 changeSizeByFileTypeChart,
		 filesInTheSameCommitGraph,
		 committersChangingSameFilesGraph,
		 amountOfCommitsTreemap,
		 commitTimePunchcard,
		 timeBetweenCommitsHistogram,
		 commitMessageWordCloud
		].each{ visualization ->
			visualization.generate(context)
			visualization.measure.forEachDuration{
				println(it)
			}
		}
	}
}
