import analysis.Analysis
import com.intellij.ide.BrowserUtil
import com.intellij.ide.GeneralSettings
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.ui.UIUtil
import events.EventStorage
import historyreader.*
import http.HttpUtil
import org.jetbrains.annotations.Nullable
import ui.DialogState
import ui.FileAmountToolWindow
import util.Measure

import static IntegrationTestsRunner.runIntegrationTests
import static com.intellij.openapi.ui.Messages.showWarningDialog
import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static intellijeval.PluginUtil.*
import static ui.Dialog.showDialog
import static util.Measure.measure

if (false) return showFileAmountByType(project)
if (false) return CommitMunging_Playground.playOnIt()
if (false) return runIntegrationTests(project, [TextCompareProcessorTest, CommitReaderGitTest, ChangeEventsReaderGitTest])


def actionGroup = new ActionGroup("Code History Mining", true) {
	@Override AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
		def grabHistory = new AnAction("Grab Project History") {
			@Override void actionPerformed(AnActionEvent event) { grabHistoryOf(event.project) }
		}
		def projectStats = new AnAction("Amount of Files in Project") {
			@Override void actionPerformed(AnActionEvent event) { showFileAmountByType(event.project) }
		}
		[grabHistory, projectStats, new Separator()] + filesWithCodeHistory().collect{ createActionGroup(it, pathToTemplates()) }
	}
}

registerAction("CodeHistoryMiningMenu", "", "ToolsMenu", actionGroup)
registerAction("CodeHistoryMiningPopup", "alt shift H") { AnActionEvent actionEvent ->
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

		def browserConfiguredCorrectly = new File(GeneralSettings.instance.browserPath).exists()
		if (!browserConfiguredCorrectly) {
			UIUtil.invokeLaterIfNeeded {
				showWarningDialog(
						"It seems that browser is not configured correctly.\nPlease check Settings -> Web Browsers config.",
						"Code History Mining"
				)
			}
			// don't return to try to open url anyway in case the above check is wrong
		}
		BrowserUtil.launchBrowser("http://localhost:${server.port}/$template")
	}

	new DefaultActionGroup(file.name, true).with {
		add(new AnAction("Change Size Chart") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating change size chart") {
					showInBrowser("changes-size-chart.html", Analysis.&createJson_ChangeSize_Chart)
				}
			}
		})
		add(new AnAction("Changes Calendar View") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating changes calendar view") {
					showInBrowser("calendar-view.html", Analysis.&createJson_ChangeSize_Calendar)
				}
			}
		})
		add(new AnAction("Amount Of Commits Treemap") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating amount of commits treemap") {
					// TODO try sunburst layout? (http://bl.ocks.org/mbostock/4063423)
					showInBrowser("treemap.html", Analysis.TreeMapView.&createJson_AmountOfChangeInFolders_TreeMap)
				}
			}
		})
		add(new AnAction("Files In The Same Commit Graph") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating files in the same commit graph") {
					showInBrowser("files-in-same-commit-graph.html", Analysis.&createJson_FilesInTheSameCommit_Graph)
				}
			}
		})
		add(new AnAction("Committers Changing Same Files Graph") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating committers changing same files graph") {
					showInBrowser("author-to-file-graph.html", Analysis.&createJson_AuthorConnectionsThroughChangedFiles_Graph)
				}
			}
		})
		add(new AnAction("Commit Time Punchcard") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating commit time punchcard") {
					showInBrowser("commit-time-punchcard.html", Analysis.&createJson_CommitsByDayOfWeekAndTime_PunchCard)
				}
			}
		})
		add(new AnAction("Time Between Commits Histogram") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating time between commits histogram") {
					showInBrowser("time-between-commits-histogram.html", Analysis.&createJson_TimeBetweenCommits_Histogram)
				}
			}
		})
		add(new AnAction("Commit Messages Word Cloud") {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating commit messages word cloud") {
					showInBrowser("wordcloud.html", Analysis.&createJson_CommitComments_WordCloud)
				}
			}
		})
		add(new Separator())
		add(new AnAction("Show in File Manager") {
			@Override void actionPerformed(AnActionEvent event) {
				ShowFilePathAction.openFile(file)
			}
		})
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
	if (CommitReader.amountOfVCSRootsIn(project) == 0) {
		showWarningDialog(project, "Cannot grab project history because there are no VCS roots setup for it.", "Code History Mining")
		return
	} else if (CommitReader.amountOfVCSRootsIn(project) > 1) {
		showWarningDialog(project, "Sorry, grabbing history for project with multiple VCS roots is not implemented.", "Code History Mining")
		return
	}

	def state = DialogState.loadDialogStateFor(project, dialogStatePath()) {
		def outputFilePath = "${pathToHistoryFiles()}/${project.name + "-file-events.csv"}"
		new DialogState(new Date() - 300, new Date(), outputFilePath, false)
	}
	showDialog(state, "Grab History Of Current Project", project) { DialogState userInput ->
		DialogState.saveDialogStateOf(project, dialogStatePath(), userInput)

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

				def vcsRequestBatchSizeInDays = 1 // from personal observation this request size seems to be ok (hardcoded so that not to clutter UI dialog)
				def eventsReader = new ChangeEventsReader(
						new CommitReader(project, vcsRequestBatchSizeInDays),
						new CommitFilesMunger(project, userInput.grabChangeSizeInLines).&mungeCommit
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
				def message = "Saved change events to ${storage.filePath}\n" +
						"It should have history from '${storage.oldestEventTime}' to '${storage.mostRecentEventTime}'."
				showInConsole(message, consoleTitle, project)
			}
			Measure.durations.entrySet().collect{ it.key + ": " + it.value }.each{ log(it) }
		}
	}
}

def showFileAmountByType(Project project) {
	def scope = GlobalSearchScope.projectScope(project)
	def fileCountByFileExtension = FileTypeManager.instance.registeredFileTypes.inject([:]) { Map map, FileType fileType ->
		int fileCount = FileBasedIndex.instance.getContainingFiles(FileTypeIndex.NAME, fileType, scope).size()
		if (fileCount > 0) map.put(fileType.defaultExtension, fileCount)
		map
	}.sort{ -it.value }

	FileAmountToolWindow.showIn(project, fileCountByFileExtension)
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

String pathToTemplates() { new File(this.class.classLoader.getResource("templates").toURI()).absolutePath }

String dialogStatePath() { "${PathManager.pluginsPath}/code-history-mining" }

static String pathToHistoryFiles() { "${PathManager.pluginsPath}/code-history-mining" }

static String projectName(File file) {
	file.name.replace(".csv", "").replace("-file-events", "")
}