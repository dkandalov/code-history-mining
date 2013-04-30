import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import history.EventStorage
import history.Measure
import history.ProjectHistory
import history.events.ChangeEvent
import org.jetbrains.annotations.Nullable

import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static history.ChangeExtractor.changeEventsFrom
import static history.Measure.measure
import static intellijeval.PluginUtil.*

if (isIdeStartup) return

//new TextCompareProcessorTestSuite(project).run()
//if (true) return


doInBackground("Analyzing project history", { ProgressIndicator indicator ->
	measure("time") {
		def storage = new EventStorage("${PathManager.pluginsPath}/delta-flora/${project.name}-events.csv")

		def now = new Date()
		def daysOfHistory = 900
		def sizeOfVCSRequestInDays = 1

		if (storage.hasNoEvents()) {
			def historyStart = now - daysOfHistory
			def historyEnd = now

			log("Loading project history from $historyStart to $historyEnd")
			Iterator<CommittedChangeList> changeLists = ProjectHistory.fetchChangeListsFor(project, historyStart, historyEnd, sizeOfVCSRequestInDays)
			processChangeLists(changeLists, indicator) { changeEvents ->
				storage.appendToEventsFile(changeEvents)
			}
		} else {
			def historyStart = storage.mostRecentEventTime
			def historyEnd = now
			log("Loading project history from $historyStart to $historyEnd")

			def changeLists = ProjectHistory.fetchChangeListsFor(project, historyStart, historyEnd, sizeOfVCSRequestInDays, false)
			processChangeLists(changeLists, indicator) { changeEvents ->
				storage.prependToEventsFile(changeEvents)
			}

			historyStart = now - daysOfHistory
			historyEnd = storage.oldestEventTime
			log("Loading project history from $historyStart to $historyEnd")

			changeLists = ProjectHistory.fetchChangeListsFor(project, historyStart, historyEnd, sizeOfVCSRequestInDays)
			processChangeLists(changeLists, indicator) { changeEvents ->
				storage.appendToEventsFile(changeEvents)
			}
		}

		showInConsole("Saved change events to ${storage.filePath}", "output", project)
		showInConsole("(it should have history from '${storage.oldestEventTime}' to '${storage.mostRecentEventTime}')", "output", project)
	}
	Measure.durations.entrySet().collect{ "Total " + it.key + ": " + it.value }.each{ log(it) }
}, {})

def processChangeLists(changeLists, indicator, callback) {
	for (changeList in changeLists) {
		if (changeList == null) break
		if (indicator.canceled) break
		log(changeList.name)

		def date = dateFormat.format((Date) changeList.commitDate)
		indicator.text = "Analyzing project history (${date} - '${changeList.comment.trim()}')"
		catchingAll_ {
//			Collection<ChangeEvent> changeEvents = fileChangeEventsFrom((CommittedChangeList) changeList, project)
			Collection<ChangeEvent> changeEvents = changeEventsFrom((CommittedChangeList) changeList, project)
			callback(changeEvents)
		}
		indicator.text = "Analyzing project history (${date} - looking for next commit...)"
	}
}

@Nullable static <T> T catchingAll_(Closure<T> closure) {
	try {
		closure.call()
	} catch (Exception e) {
		log(e)
		null
	}
}



