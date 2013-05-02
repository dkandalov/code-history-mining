import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import history.ChangeEventsExtractor
import history.EventStorage
import history.SourceOfChangeLists
import history.util.Measure
import http.HttpUtil

import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static history.SourceOfChangeLists.getNO_MORE_CHANGE_LISTS
import static history.util.Measure.measure
import static intellijeval.PluginUtil.*

String thisPluginPath = pluginPath // TODO remove when templates are self-contained

registerAction("DeltaFloraPopup", "ctrl alt shift D") { AnActionEvent actionEvent ->
	JBPopupFactory.instance.createActionGroupPopup(
			"Delta Flora",
			new DefaultActionGroup().with {
				add(new AnAction("Grab Current Project History") {
					@Override void actionPerformed(AnActionEvent event) {
						grabHistoryOf(event.project)
					}
				})
				add(new Separator())

				def eventFiles = new File("${PathManager.pluginsPath}/delta-flora").listFiles(new FileFilter() {
					@Override boolean accept(File pathname) { pathname.name.endsWith(".csv") }
				})
				addAll(eventFiles.collect{ file -> createEventStorageActionGroup(file, thisPluginPath) })
				it
			},
			actionEvent.dataContext,
			JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
			true
	).showCenteredInCurrentWindow(actionEvent.project)
}

show("loaded DeltaFlora plugin")


static ActionGroup createEventStorageActionGroup(File file, String pluginPath) {
	new DefaultActionGroup(file.name, true).with {
		String projectName = file.name.replace(".csv", "")
		add(new AnAction("Change Size Calendar View") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating calendar view", {
					def filePath = "${PathManager.pluginsPath}/delta-flora/${projectName}.csv"
					def events = new EventStorage(filePath).readAllEvents()
					def json = Analysis.createJsonForCalendarView(events)
					def server = HttpUtil.loadIntoHttpServer(projectName, pluginPath + "/html", "calendar_view_template.html", json)
					BrowserUtil.launchBrowser("http://localhost:${server.port}/calendar_view_template.html")
				}, {})
			}
		})
		add(new AnAction("Change Size History") {
			@Override void actionPerformed(AnActionEvent event) {
				show(file.name)
			}
		})
		add(new AnAction("Files In The Same Commit Graph") {
			@Override void actionPerformed(AnActionEvent event) {
				show(file.name)
			}
		})
		it
	}
}


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

			def updateIndicatorText = { changeList, callback ->
				log(changeList.name)
				def date = dateFormat.format((Date) changeList.commitDate)
				indicator.text = "Grabbing project history (${date} - '${changeList.comment.trim()}')"

				callback()

				indicator.text = "Grabbing project history (${date} - looking for next commit...)"
			}
			def appendToStorage = { batchOfChangeEvents -> storage.appendToEventsFile(batchOfChangeEvents) }
			def prependToStorage = { batchOfChangeEvents -> storage.prependToEventsFile(batchOfChangeEvents) }


			if (storage.hasNoEvents()) {
				def historyStart = now - daysOfHistory
				def historyEnd = now
				log("Loading project history from $historyStart to $historyEnd")
				sourceOfChangeEvents.request(historyStart, historyEnd, indicator, updateIndicatorText, appendToStorage)

			} else {
				def historyStart = storage.mostRecentEventTime
				def historyEnd = now
				log("Loading project history from $historyStart to $historyEnd")
				sourceOfChangeEvents.request(historyStart, historyEnd, indicator, updateIndicatorText, prependToStorage)

				historyStart = now - daysOfHistory
				historyEnd = storage.oldestEventTime
				log("Loading project history from $historyStart to $historyEnd")
				sourceOfChangeEvents.request(historyStart, historyEnd, indicator, updateIndicatorText, appendToStorage)
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

	def request(Date historyStart, Date historyEnd, indicator = null, Closure callbackWrapper = {it()}, Closure callback) {
		Iterator<CommittedChangeList> changeLists = sourceOfChangeLists.fetchChangeLists(historyStart, historyEnd)
		for (changeList in changeLists) {
			if (changeList == NO_MORE_CHANGE_LISTS) break
			if (indicator?.canceled) break

			callbackWrapper(changeList) {
				catchingAll {
					def changeEvents = eventsExtractor.changeEventsFrom(changeList)
					callback(changeEvents)
				}
			}
		}
	}
}


