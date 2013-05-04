import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import com.michaelbaranov.microba.calendar.DatePicker
import history.ChangeEventsExtractor
import history.EventStorage
import history.SourceOfChangeEvents
import history.SourceOfChangeLists
import history.util.Measure
import http.HttpUtil

import javax.swing.*
import java.awt.GridBagLayout
import java.awt.Insets

import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static history.util.Measure.measure
import static intellijeval.PluginUtil.*
import static java.awt.GridBagConstraints.*

String pathToTemplates = pluginPath + "/html"

showDialog(new Date(), new Date(), 1, "Grab History Of Current Project", project) { from, to, vcsRequestBatchSizeInDays ->
	show(from)
	show(to)
}

def showDialog(Date from, Date to, int vcsRequestBatchSizeInDays, String title, Project project, Closure onOkCallback) {
	def fromDatePicker = new DatePicker(from, dateFormat.delegate)
	def toDatePicker = new DatePicker(to, dateFormat.delegate)
	def vcsRequestSizeField = new JTextField(String.valueOf(vcsRequestBatchSizeInDays))

	JPanel rootPanel = new JPanel().with{
		layout = new GridBagLayout()
		GridBag bag = new GridBag()
		     .setDefaultAnchor(0, EAST)
		     .setDefaultAnchor(1, WEST)
		     .setDefaultWeightX(1, 1)
		     .setDefaultFill(HORIZONTAL)
		bag.defaultInsets = new Insets(5, 5, 5, 5)
		
		add(new JLabel("From:"), bag.nextLine().next())
		add(fromDatePicker, bag.next())
		add(new JLabel("To:"), bag.next())
		add(toDatePicker, bag.next())
		add(new JLabel("VCS Request batch size:"), bag.nextLine().next())
		add(vcsRequestSizeField, bag.next())
		add(new JLabel("day(s)"), bag.next().coverLine())


		def text = "(Please note that grabbing history might significantly slow down UI and/or take a really long time for a big project)"
		def textArea = new JTextArea(text).with {
			lineWrap = true
			wrapStyleWord = true
			editable = false
			it
		}
		textArea.background = background
		add(textArea, bag.nextLine().coverLine())
		it
	}

	DialogBuilder builder = new DialogBuilder(project)
	builder.title = title
	builder.okActionEnabled = true
	builder.okOperation = {
		def toInteger = {
			String s = it.trim().replaceAll("\\D", "")
			s.empty ? 1 : s.toInteger()
		}
		onOkCallback(fromDatePicker.date, toDatePicker.date, toInteger(vcsRequestSizeField.text))
		builder.dialogWrapper.close(0)
	} as Runnable
	builder.centerPanel = rootPanel

	ApplicationManager.application.invokeLater{ builder.showModal(true) } as Runnable
}
return

registerAction("DeltaFloraPopup", "ctrl alt shift D") { AnActionEvent actionEvent ->
	JBPopupFactory.instance.createActionGroupPopup(
			"Delta Flora",
			new DefaultActionGroup().with {
				add(new AnAction("Grab Project History (on method level)") {
					@Override void actionPerformed(AnActionEvent event) {
						grabHistoryOf(event.project, true)
					}
				})
				add(new AnAction("Grab Project History (on file level)") {
					@Override void actionPerformed(AnActionEvent event) {
						grabHistoryOf(event.project, false)
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

show("loaded DeltaFlora plugin")


static ActionGroup createEventStorageActionGroup(File file, String pathToTemplates) {
	String projectName = file.name.replace(".csv", "")
	def showInBroswer = { template, eventsToJson ->
		def filePath = "${PathManager.pluginsPath}/delta-flora/${projectName}.csv"
		def events = new EventStorage(filePath).readAllEvents()
		def json = eventsToJson(events)
		def server = HttpUtil.loadIntoHttpServer(projectName, pathToTemplates, template, json)
		BrowserUtil.launchBrowser("http://localhost:${server.port}/$template")
	}

	new DefaultActionGroup(file.name, true).with {
		add(new AnAction("Change Size Calendar View") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating calendar view", {
					showInBroswer("calendar_view.html", Analysis.&createJsonForCalendarView)
				}, {})
			}
		})
		add(new AnAction("Change Size History") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating change size history", {
					showInBroswer("changes_size_chart.html", Analysis.&createJsonForBarChartView)
				}, {})
			}
		})
		add(new AnAction("Files In The Same Commit Graph") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Files in the same commit graph", {
					showInBroswer("cooccurrences-graph.html", Analysis.&createJsonForCooccurrencesGraph)
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

SourceOfChangeEvents sourceOfChangeEventsFor(Project project, boolean extractEventsOnMethodLevel) {
	def vcsRequestBatchSizeInDays = 1
	def sourceOfChangeLists = new SourceOfChangeLists(project, vcsRequestBatchSizeInDays)
	def extractEvents = (extractEventsOnMethodLevel ?
		new ChangeEventsExtractor(project).&changeEventsFrom :
		new ChangeEventsExtractor(project).&fileChangeEventsFrom
	)
	new SourceOfChangeEvents(sourceOfChangeLists, extractEvents)
}

def grabHistoryOf(Project project, boolean extractEventsOnMethodLevel) {
	def sourceOfChangeEvents = sourceOfChangeEventsFor(project, extractEventsOnMethodLevel)

	doInBackground("Grabbing project history", { ProgressIndicator indicator ->
		measure("time") {
			def updateIndicatorText = { changeList, callback ->
				log(changeList.name)
				def date = dateFormat.format((Date) changeList.commitDate)
				indicator.text = "Grabbing project history (${date} - '${changeList.comment.trim()}')"

				callback()

				indicator.text = "Grabbing project history (${date} - looking for next commit...)"
			}
			def outputFile = project.name + (extractEventsOnMethodLevel ? "-events.csv" : "-events-min.csv")
			def storage = new EventStorage("${PathManager.pluginsPath}/delta-flora/${outputFile}")
			def appendToStorage = { batchOfChangeEvents -> storage.appendToEventsFile(batchOfChangeEvents) }
			def prependToStorage = { batchOfChangeEvents -> storage.prependToEventsFile(batchOfChangeEvents) }

			def now = new Date()
			def daysOfHistory = 900

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


