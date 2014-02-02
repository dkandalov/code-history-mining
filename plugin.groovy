import analysis.Analysis
import analysis.Visualization
import com.intellij.ide.BrowserUtil
import com.intellij.ide.GeneralSettings
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.ui.UIUtil
import events.EventStorage
import historyreader.*
import http.HttpUtil
import http.Template
import org.jetbrains.annotations.Nullable
import ui.DialogState
import ui.FileAmountToolWindow
import util.CancelledException
import util.Measure

import static com.intellij.openapi.ui.Messages.showWarningDialog
import static liveplugin.PluginUtil.*
import static ui.Dialog.showDialog
import static util.Measure.measure

//noinspection GroovyConstantIfStatement
if (false) return showFileAmountByType(project)
//noinspection GroovyConstantIfStatement
if (false) return CommitMunging_Playground.playOnIt()


def actionGroup = new ActionGroup("Code History Mining", true) {
	@Override AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
		def grabHistory = new AnAction("Grab Project History") {
			@Override void actionPerformed(AnActionEvent event) { grabHistoryOf(event.project) }
		}
		def projectStats = new AnAction("Amount of Files in Project") {
			@Override void actionPerformed(AnActionEvent event) { showFileAmountByType(event.project) }
		}
		[grabHistory, projectStats, new Separator()] + filesWithCodeHistory().collect{ createActionGroup(it) }
	}
}
registerAction("CodeHistoryMiningMenu", "", "VcsGroups", "Code History Mining", actionGroup)

registerAction("CodeHistoryMiningPopup", "alt shift H") { AnActionEvent actionEvent ->
	JBPopupFactory.instance.createActionGroupPopup(
			"Code History Mining", actionGroup, actionEvent.dataContext, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, true
	).showCenteredInCurrentWindow(actionEvent.project)
}

if (!isIdeStartup) show("Reloaded code-history-mining plugin")


static AnAction createActionGroup(File file) {
	def showInBrowser = { templateFileName, eventsToJson, checkIfCancelled ->
		def events = measure("Storage.readAllEvents") {
			new EventStorage(file.absolutePath).readAllEvents(checkIfCancelled){ line, e -> log_("Failed to parse line '${line}'") }
		}

		def json = eventsToJson(events, checkIfCancelled)

		def text = new Template(HttpUtil.readFile(templateFileName))
				.inlineImports(HttpUtil.&readFile)
				.fillProjectName(projectName(file))
				.fillData(json)
				.text
		def server = HttpUtil.loadIntoHttpServer(text, projectName(file), templateFileName)

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
		BrowserUtil.launchBrowser("http://localhost:${server.port}/$templateFileName")
	}

	def createAction = { String name, templateFile, processing ->
		new AnAction(name) {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating ${name.toLowerCase()}") { ProgressIndicator indicator ->
					try {
						Measure.reset()

						showInBrowser(templateFile, processing, CancelledException.check(indicator))

						Measure.forEachDuration{ log_(it) }
					} catch (CancelledException ignored) {
						log_("Cancelled building '${name}'")
					}
				}
			}
		}
	}
	def projectName = projectName(file)
	def createAction_NEW = { Visualization visualization ->
		new AnAction(visualization.name) {
			@Override void actionPerformed(AnActionEvent event) {
				doInBackground("Creating ${visualization.name.toLowerCase()}") { ProgressIndicator indicator ->
					try {
						Measure.reset()

						def checkIfCancelled = CancelledException.check(indicator)
						def events = measure("Storage.readAllEvents") {
							new EventStorage(file.absolutePath).readAllEvents(checkIfCancelled){ line, e -> log_("Failed to parse line '${line}'") }
						}
						def context = new Visualization.Context(events, projectName, checkIfCancelled)
						def html = visualization.generate(context)

						def url = HttpUtil.loadIntoHttpServer_NEW(html, projectName, visualization.name + ".html")

						// TODO seems like com.intellij.ide.BrowserUtil.browse already shows a message for misconfigured browser path
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
						BrowserUtil.launchBrowser(url)

						Measure.forEachDuration{ log_(it) }
					} catch (CancelledException ignored) {
						log_("Cancelled building '${visualization.name}'")
					}
				}
			}
		}
	}
	new DefaultActionGroup(file.name, true).with {
		add(createAction_NEW(Visualization.changeSizeChart))
		add(createAction(
				"Amount Of Committers Chart",
				"amount-of-committers-chart.html", Analysis.&createJson_AmountOfCommitters_Chart))
		add(createAction(
				"Amount Of Files In Commit Chart",
				"amount-of-files-in-commit-chart.html", Analysis.&createJson_AverageAmountOfFilesInCommit_Chart))
		add(createAction(
				"Amount Of Commits Treemap",
				"treemap.html", Analysis.TreeMapView.&createJson_AmountOfChangeInFolders_TreeMap)) // TODO try sunburst layout? (http://bl.ocks.org/mbostock/4063423)
		add(createAction(
				"Files In The Same Commit Graph",
				"files-in-same-commit-graph.html", Analysis.&createJson_FilesInTheSameCommit_Graph))
		add(createAction(
				"Committers Changing Files Graph",
				"committers-changing-files-graph.html", Analysis.&committersChangingFilesGraph))
		add(createAction(
				"Committers Changing Same Files Graph",
				"committers-changing-same-files-graph.html", Analysis.&createJson_AuthorConnectionsThroughChangedFiles_Graph))
		add(createAction(
				"Commit Time Punchcard",
				"commit-time-punchcard.html", Analysis.&createJson_CommitsByDayOfWeekAndTime_PunchCard))
		add(createAction(
				"Time Between Commits Histogram",
				"time-between-commits-histogram.html", Analysis.&createJson_TimeBetweenCommits_Histogram))
		add(createAction(
				"Commit Messages Word Cloud",
				"wordcloud.html", Analysis.&createJson_CommitComments_WordCloud))
		add(new Separator())
		add(new AnAction("Show in File Manager") {
			@Override void actionPerformed(AnActionEvent event) {
				ShowFilePathAction.openFile(file)
			}
		})
		add(new AnAction("Rename") {
			@Override void actionPerformed(AnActionEvent event) {
				def newFileName = Messages.showInputDialog("New file name:", "Rename File", null, file.name, new InputValidator() {
					@Override boolean checkInput(String newFileName) { newFileName.length() > 0 && !new File(file.parent + "/" + newFileName).exists() }
					@Override boolean canClose(String newFileName) { true }
				})
				if (newFileName != null) {
					FileUtil.rename(file, new File(file.parent + "/" + newFileName))
				}
			}
		})
		add(new AnAction("Delete") {
			@Override void actionPerformed(AnActionEvent event) {
				int userAnswer = Messages.showOkCancelDialog("Delete ${file.name}?", "Delete File", "&Delete", "&Cancel", UIUtil.getQuestionIcon())
				if (userAnswer == Messages.OK) FileUtil.delete(file)
			}
		})
		it
	}
}

def grabHistoryOf(Project project) {
	if (CommitReader.noVCSRootsIn(project)) {
		showWarningDialog(project, "Cannot grab project history because there are no VCS roots setup for it.", "Code History Mining")
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
				def storage = new EventStorage(userInput.outputFilePath)
				def vcsRequestBatchSizeInDays = 1 // based on personal observation (hardcoded so that not to clutter UI dialog)
				def eventsReader = new ChangeEventsReader(
						new CommitReader(project, vcsRequestBatchSizeInDays),
						new CommitFilesMunger(project, userInput.grabChangeSizeInLines).&mungeCommit
				)

				def message = HistoryGrabber.doGrabHistory(eventsReader, storage, userInput, indicator)

				showInConsole(message.text, message.title, project)
			}
			Measure.forEachDuration{ log_(it) }
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

static File[] filesWithCodeHistory() {
	new File(pathToHistoryFiles()).listFiles(new FileFilter() {
		@Override boolean accept(File pathName) { pathName.name.endsWith(".csv") }
	})
}

static log_(String message) {
	Logger.getInstance("CodeHistoryMining").info(message)
}

String dialogStatePath() {
	"${PathManager.pluginsPath}/code-history-mining"
}

static String pathToHistoryFiles() {
	"${PathManager.pluginsPath}/code-history-mining"
}

static String projectName(File file) {
	file.name.replace(".csv", "").replace("-file-events", "")
}