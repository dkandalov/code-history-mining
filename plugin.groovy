import analysis.Analysis
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import history.*
import history.events.EventStorage
import history.util.Measure
import http.HttpUtil
import org.jetbrains.annotations.Nullable
import ui.DialogState

import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.*

import static IntegrationTestsRunner.runIntegrationTests
import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static history.util.Measure.measure
import static intellijeval.PluginUtil.*
import static java.awt.GridBagConstraints.BOTH
import static java.awt.GridBagConstraints.NORTH
import static ui.Dialog.showDialog

def pathToTemplates = pluginPath + "/templates"

if (false) return showProjectStatistics(project)
if (false) return CommitMunging_Playground.playOnIt()
if (false) return runIntegrationTests(project, [TextCompareProcessorTest, CommitReaderGitTest, ChangeEventsReaderGitTest])


def actionGroup = new ActionGroup("Code History Mining", true) {
	@Override AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
		def grabHistory = new AnAction("Grab Project History") {
			@Override void actionPerformed(AnActionEvent event) { grabHistoryOf(event.project) }
		}
		def projectStats = new AnAction("Show Project Statistics") {
			@Override void actionPerformed(AnActionEvent event) { showProjectStatistics(event.project) }
		}
		[grabHistory, projectStats, new Separator()] + filesWithCodeHistory().collect{ createActionGroup(it, pathToTemplates) }
	}
}

registerAction("CodeHistoryMiningMenu", "", "ToolsMenu", actionGroup)
registerAction("CodeHistoryMiningPopup", "ctrl alt shift D") { AnActionEvent actionEvent ->
	JBPopupFactory.instance.createActionGroupPopup(
			"Code History Mining", actionGroup, actionEvent.dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true
	).showCenteredInCurrentWindow(actionEvent.project)
}

if (!isIdeStartup) show("Reloaded code-history-mining plugin")


static AnAction createActionGroup(File file, String pathToTemplates) {
	def showInBrowser = { template, eventsToJson ->
		def events = new EventStorage(file.absolutePath).readAllEvents { line, e -> log("Failed to parse line '${line}'") }
		def json = eventsToJson(events)

		def server = HttpUtil.loadIntoHttpServer(projectName(file), pathToTemplates, template, json)

		BrowserUtil.launchBrowser("http://localhost:${server.port}/$template")
	}

	new DefaultActionGroup(file.name, true).with {
		add(new AnAction("Change Size Calendar View") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating calendar view") {
					showInBrowser("calendar-view.html", Analysis.&createJsonForCalendarView)
				}
			}
		})
		add(new AnAction("Change Size Chart") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating change size chart") {
					showInBrowser("changes-size-chart.html", Analysis.&createJsonForBarChartView)
				}
			}
		})
		add(new AnAction("Files In The Same Commit Graph") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Files in the same commit graph") {
					showInBrowser("files-in-same-commit-graph.html", Analysis.&createJson_FilesInTheSameCommit_Graph)
				}
			}
		})
		add(new AnAction("Committers Changing Same Files Graph") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Committers changing same files graph") {
					showInBrowser("author-to-file-graph.html", Analysis.&createJson_AuthorConnectionsThroughChangedFiles_Graph)
				}
			}
		})
		add(new AnAction("Changes By Package Tree Map") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Changes By Package Tree Map") {
					// TODO add sunburst layout? (http://bl.ocks.org/mbostock/4063423)
					showInBrowser("treemap.html", Analysis.TreeMapView.&createJsonForChangeSizeTreeMap)
				}
			}
		})
		add(new AnAction("Commit Messages Word Cloud") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Commit Messages Word Cloud") {
					showInBrowser("wordcloud.html", Analysis.&createJsonForCommitCommentWordCloud)
				}
			}
		})
		add(new AnAction("Commit Time Punchcard") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Commit Time Punchcard") {
					showInBrowser("commit-time-punchcard.html", Analysis.&createJson_CommitsByDayOfWeekAndTime_PunchCard)
				}
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
	def state = DialogState.loadDialogStateFor(project, pluginPath) {
		def outputFilePath = "${pathToHistoryFiles()}/${project.name + "-file-events.csv"}"
		new DialogState(new Date() - 300, new Date(), 1, outputFilePath)
	}
	showDialog(state, "Grab History Of Current Project", project) { DialogState userInput ->
		DialogState.saveDialogStateOf(project, pluginPath, userInput)

		doInBackground("Grabbing project history") { ProgressIndicator indicator ->
			measure("Total time") {
				def updateIndicatorText = { changeList, callback ->
					log(changeList.name)
					def date = dateFormat.format((Date) changeList.commitDate)
					indicator.text = "Grabbing project history (${date} - '${changeList.comment.trim()}')"

					callback()

					indicator.text = "Grabbing project history (${date} - looking for next commit...)"
				}
				def storage = new EventStorage(userInput.outputFilePath)
				def appendToStorage = { commitChangeEvents -> storage.appendToEventsFile(commitChangeEvents) }
				def prependToStorage = { commitChangeEvents -> storage.prependToEventsFile(commitChangeEvents) }

				def eventsReader = new ChangeEventsReader(
						new CommitReader(project, userInput.vcsRequestBatchSizeInDays),
						new CommitFilesMunger(project, true).&mungeCommit
				)
				def fromDate = userInput.from
				def toDate = userInput.to + 1 // "+1" add a day to make date in UI inclusive

				if (storage.hasNoEvents()) {
					log("Loading project history from ${fromDate} to ${toDate}")
					eventsReader.readPresentToPast(fromDate, toDate, indicator, updateIndicatorText, appendToStorage)

				} else {
					if (toDate > timeAfterMostRecentEventIn(storage)) {
						def (historyStart, historyEnd) = [timeAfterMostRecentEventIn(storage), toDate]
						log("Loading project history from $historyStart to $historyEnd")
						// read events from past into future because they are prepended to storage
						eventsReader.readPastToPresent(historyStart, historyEnd, indicator, updateIndicatorText, prependToStorage)
					}

					if (fromDate < timeBeforeOldestEventIn(storage)) {
						def (historyStart, historyEnd) = [fromDate, timeBeforeOldestEventIn(storage)]
						log("Loading project history from $historyStart to $historyEnd")
						eventsReader.readPresentToPast(historyStart, historyEnd, indicator, updateIndicatorText, appendToStorage)
					}
				}

				def consoleTitle = "Code History Mining"
				showInConsole("Saved change events to ${storage.filePath}", consoleTitle, project)
				showInConsole("(it should have history from '${storage.oldestEventTime}' to '${storage.mostRecentEventTime}')", consoleTitle, project)
			}
			Measure.durations.entrySet().collect{ it.key + ": " + it.value }.each{ log(it) }
		}
	}
}

def showProjectStatistics(Project project) {
	def scope = GlobalSearchScope.projectScope(project)
	def fileCountByFileExtension = FileTypeManager.instance.registeredFileTypes.inject([:]) { Map map, FileType fileType ->
		int fileCount = FileBasedIndex.instance.getContainingFiles(FileTypeIndex.NAME, fileType, scope).size()
		if (fileCount > 0) map.put(fileType.defaultExtension, fileCount)
		map
	}.sort{ -it.value }
	def totalAmountOfFiles = fileCountByFileExtension.entrySet().sum(0){ it.value }

	def actionGroup = new DefaultActionGroup().with{
		add(new AnAction(AllIcons.Actions.Cancel) {
			@Override void actionPerformed(AnActionEvent event) {
				unregisterToolWindow("Project Statistics")
			}
		})
		it
	}

	def createToolWindowPanel = {
		JPanel rootPanel = new JPanel().with{
			def tableModel = new DefaultTableModel() {
				@Override boolean isCellEditable(int row, int column) { false }
			}
			tableModel.addColumn("File extension")
			tableModel.addColumn("File count")
			fileCountByFileExtension.entrySet().each {
				tableModel.addRow([it.key, it.value].toArray())
			}
			tableModel.addRow(["Total", totalAmountOfFiles].toArray())
			def table = new JBTable(tableModel).with { // TODO make table content copyable
				striped = true
				showGrid = false
				it
			}

			layout = new GridBagLayout()
			GridBag bag = new GridBag().setDefaultWeightX(1).setDefaultWeightY(1).setDefaultFill(BOTH)
			add(new JBScrollPane(table), bag.nextLine().next().anchor(NORTH))

			it
		}

		def toolWindowPanel = new SimpleToolWindowPanel(true)
		toolWindowPanel.content = rootPanel
		toolWindowPanel.toolbar = ActionManager.instance.createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, actionGroup, true).component
		toolWindowPanel
	}

	def toolWindow = registerToolWindowIn(project, "Project Statistics", createToolWindowPanel(), ToolWindowAnchor.RIGHT)
	def doNothing = {} as Runnable
	toolWindow.show(doNothing)
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

static File[] filesWithCodeHistory() {
	new File(pathToHistoryFiles()).listFiles(new FileFilter() {
		@Override boolean accept(File pathName) { pathName.name.endsWith(".csv") }
	})
}

static String pathToHistoryFiles() { "${PathManager.pluginsPath}/code-history-mining" }

static String projectName(File file) {
	file.name.replace(".csv", "").replace("-file-events", "")
}