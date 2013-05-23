import analysis.Analysis
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.ui.UIUtil
import history.*
import history.events.EventStorage
import history.util.Measure
import http.HttpUtil
import ui.DialogState

import static IntegrationTestsRunner.runIntegrationTests
import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static history.util.Measure.measure
import static intellijeval.PluginUtil.*
import static ui.Dialog.showDialog

String pathToTemplates = pluginPath + "/templates"

if (false) return CommitMunging_Playground.playOnIt()
if (true) return runIntegrationTests(project, [TextCompareProcessorTest, CommitReaderGitTest, ChangeEventsReaderGitTest])

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

				def eventFiles = new File("${PathManager.pluginsPath}/delta-flora").listFiles(new FileFilter() {
					@Override boolean accept(File pathName) { pathName.name.endsWith(".csv") }
				})
				addAll(eventFiles.collect{ file -> createEventStorageActionGroup(file, pathToTemplates) })
				it
			},
			actionEvent.dataContext,
			JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
			true
	).showCenteredInCurrentWindow(actionEvent.project)
}
if (!isIdeStartup) show("reloaded DeltaFlora plugin")


static ActionGroup createEventStorageActionGroup(File file, String pathToTemplates) {
	String projectName = file.name.replace(".csv", "")
	def showInBrowser = { template, eventsToJson ->
		def filePath = "${PathManager.pluginsPath}/delta-flora/${projectName}.csv"
		def events = new EventStorage(filePath).readAllEvents { line, e -> log("Failed to parse line '${line}'") }
		def json = eventsToJson(events)
		def server = HttpUtil.loadIntoHttpServer(projectName, pathToTemplates, template, json)
		BrowserUtil.launchBrowser("http://localhost:${server.port}/$template")
	}

	new DefaultActionGroup(file.name, true).with {
		add(new AnAction("Change Size Calendar View") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating calendar view", {
					showInBrowser("calendar_view.html", Analysis.&createJsonForCalendarView)
				}, {})
			}
		})
		add(new AnAction("Change Size History") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating change size history", {
					showInBrowser("changes_size_chart.html", Analysis.&createJsonForBarChartView)
				}, {})
			}
		})
		add(new AnAction("Files In The Same Commit Graph") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Files in the same commit graph", {
					showInBrowser("cooccurrences-graph.html", Analysis.&createJsonForCooccurrencesGraph)
				}, {})
			}
		})
		add(new AnAction("Changes By Package Tree Map") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Changes By Package Tree Map", {
					showInBrowser("treemap.html", Analysis.TreeMap.&createJsonForChangeSizeTreeMap)
				}, {})
			}
		})
		add(new AnAction("Commit Messages Word Cloud") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Commit Messages Word Cloud", {
					showInBrowser("wordcloud.html", Analysis.&createJsonForCommitCommentWordCloud)
				}, {})
			}
		})
		add(new Separator())
		add(new AnAction("Delete") {
			@Override void actionPerformed(AnActionEvent event) {
				int userAnswer = Messages.showOkCancelDialog("Delete ${file.name}?", "Delete File", "&Delete", "&Cancel", UIUtil.getQuestionIcon());
				if (userAnswer == Messages.OK) file.delete()
			}
		})
		it
	}
}

def grabHistoryOf(Project project) {
	def eventsReader = changeEventsReaderFor(project)

	def state = DialogState.loadDialogStateFor(project, pluginPath) {
		def outputFile = project.name + "-events.csv"
		def outputFilePath = "${PathManager.pluginsPath}/delta-flora/${outputFile}"
		new DialogState(new Date() - 300, new Date(), 1, outputFilePath)
	}
	showDialog(state, "Grab History Of Current Project", project) { DialogState userInput ->
		DialogState.saveDialogStateOf(project, pluginPath, userInput)

		doInBackground("Grabbing project history", { ProgressIndicator indicator ->
			measure("time") {
				def updateIndicatorText = { changeList, callback ->
					log(changeList.name)
					def date = dateFormat.format((Date) changeList.commitDate)
					indicator.text = "Grabbing project history (${date} - '${changeList.comment.trim()}')"

					callback()

					indicator.text = "Grabbing project history (${date} - looking for next commit...)"
				}
				def storage = new EventStorage(userInput.outputFilePath)
				def appendToStorage = { batchOfChangeEvents -> storage.appendToEventsFile(batchOfChangeEvents) }
				def prependToStorage = { batchOfChangeEvents -> storage.prependToEventsFile(batchOfChangeEvents) }

				if (storage.hasNoEvents()) {
					log("Loading project history from ${userInput.from} to ${userInput.to}")
					eventsReader.request(userInput.from, userInput.to, indicator, updateIndicatorText, appendToStorage)

				} else {
					def historyStart = timeAfterMostRecentEventIn(storage)
					def historyEnd = userInput.to
					log("Loading project history from $historyStart to $historyEnd")
					eventsReader.request(historyStart, historyEnd, indicator, updateIndicatorText, prependToStorage)

					historyStart = userInput.from
					historyEnd = timeBeforeOldestEventIn(storage)
					log("Loading project history from $historyStart to $historyEnd")
					eventsReader.request(historyStart, historyEnd, indicator, updateIndicatorText, appendToStorage)
				}

				showInConsole("Saved change events to ${storage.filePath}", "output", project)
				showInConsole("(it should have history from '${storage.oldestEventTime}' to '${storage.mostRecentEventTime}')", "output", project)
			}
			Measure.durations.entrySet().collect{ "Total " + it.key + ": " + it.value }.each{ log(it) }
		}, {})
	}
}

ChangeEventsReader changeEventsReaderFor(Project project) {
	def vcsRequestBatchSizeInDays = 1
	def commitReader = new CommitReader(project, vcsRequestBatchSizeInDays)
	new ChangeEventsReader(commitReader, new CommitFilesMunger(project).&mungeCommit)
}

static timeBeforeOldestEventIn(EventStorage storage) {
	def date = storage.oldestEventTime
	if (date == null) {
		new Date()
	} else {
		// minus one second because git "before" seems to be inclusive (even though ChangeBrowserSettings API is exclusive)
		// (it means that if processing stops between two commits that happened on the same second,
		// we will miss one of them.. considered this to be insignificant)
		date.time -= 1000
		date
	}
}

static timeAfterMostRecentEventIn(EventStorage storage) {
	def date = storage.mostRecentEventTime
	if (date == null) {
		new Date()
	} else {
		date.time += 1000  // plus one second (see comments in timeBeforeOldestEventIn())
		date
	}
}
