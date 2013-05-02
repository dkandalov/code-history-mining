import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import history.ChangeEventsExtractor
import history.EventStorage
import history.SourceOfChangeLists
import history.events.ChangeEvent
import history.util.Measure

import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static history.SourceOfChangeLists.getNO_MORE_CHANGE_LISTS
import static history.util.Measure.measure
import static intellijeval.PluginUtil.*

registerAction("DeltaFloraPopup", "ctrl alt shift D") { AnActionEvent actionEvent ->
	JBPopupFactory.instance.createActionGroupPopup(
			"Delta Flora",
			new DefaultActionGroup().with {
				add(new AnAction("Grab Project History") {
					@Override void actionPerformed(AnActionEvent event) {
						grabHistoryOf(event.project)
					}
				})
				add(new Separator())
				add(new AnAction("some.csv") {
					@Override void actionPerformed(AnActionEvent event) {
						show("oooo") // TODO list existing .csv files with actions on them
					}
				})
				it
			},
			actionEvent.dataContext,
			JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
			true
	).showCenteredInCurrentWindow(actionEvent.project)
}
show("loaded DeltaFlora plugin")


def grabHistoryOf(Project project) {

	doInBackground("Grabbing project history", { ProgressIndicator indicator ->
		measure("time") {
			def storage = new EventStorage("${PathManager.pluginsPath}/delta-flora/${project.name}-events.csv")

			def now = new Date()
			def daysOfHistory = 900
			def sizeOfVCSRequestInDays = 1
			def sourceOfChangeLists = new SourceOfChangeLists(project, sizeOfVCSRequestInDays)
			def eventsExtractor = new ChangeEventsExtractor(project)
			def sourceOfChangeEvents = new SourceOfChangeEvents(sourceOfChangeLists, eventsExtractor)

			if (storage.hasNoEvents()) {
				def historyStart = now - daysOfHistory
				def historyEnd = now

				log("Loading project history from $historyStart to $historyEnd")
				Iterator<CommittedChangeList> changeLists = sourceOfChangeLists.fetchChangeLists(historyStart, historyEnd)
				processChangeLists(changeLists, indicator, project) { changeEvents ->
					storage.appendToEventsFile(changeEvents)
				}
			} else {
				def historyStart = storage.mostRecentEventTime
				def historyEnd = now
				log("Loading project history from $historyStart to $historyEnd")

				def changeLists = sourceOfChangeLists.fetchChangeLists(historyStart, historyEnd, false)
				processChangeLists(changeLists, indicator, project) { changeEvents ->
					storage.prependToEventsFile(changeEvents)
				}

				historyStart = now - daysOfHistory
				historyEnd = storage.oldestEventTime
				log("Loading project history from $historyStart to $historyEnd")

				changeLists = sourceOfChangeLists.fetchChangeLists(historyStart, historyEnd)
				processChangeLists(changeLists, indicator, project) { changeEvents ->
					storage.appendToEventsFile(changeEvents)
				}
			}

			showInConsole("Saved change events to ${storage.filePath}", "output", project)
			showInConsole("(it should have history from '${storage.oldestEventTime}' to '${storage.mostRecentEventTime}')", "output", project)
		}
		Measure.durations.entrySet().collect{ "Total " + it.key + ": " + it.value }.each{ log(it) }
	}, {})
}

class SourceOfChangeEvents {
	private final SourceOfChangeLists sourceOfChangeLists
	private final ChangeEventsExtractor eventsExtractor

	SourceOfChangeEvents(SourceOfChangeLists sourceOfChangeLists, ChangeEventsExtractor eventsExtractor) {
		this.sourceOfChangeLists = sourceOfChangeLists
		this.eventsExtractor = eventsExtractor
	}

	def request(Date historyStart, Date historyEnd, Closure callback) {
		Iterator<CommittedChangeList> changeLists = sourceOfChangeLists.fetchChangeLists(historyStart, historyEnd)
		for (changeList in changeLists) {
			callback(eventsExtractor.changeEventsFrom(changeList))
		}
	}
}

static def processChangeLists(changeLists, indicator, project, callback) {
	for (changeList in changeLists) {
		if (changeList == NO_MORE_CHANGE_LISTS) break
		if (indicator.canceled) break
		log(changeList.name)

		def date = dateFormat.format((Date) changeList.commitDate)
		indicator.text = "Grabbing project history (${date} - '${changeList.comment.trim()}')"
		catchingAll {
//			Collection<ChangeEvent> changeEvents = new ChangeEventsExtractor(project).fileChangeEventsFrom((CommittedChangeList) changeList)
			Collection<ChangeEvent> changeEvents = new ChangeEventsExtractor(project).changeEventsFrom((CommittedChangeList) changeList)
			callback(changeEvents)
		}
		indicator.text = "Grabbing project history (${date} - looking for next commit...)"
	}
}



