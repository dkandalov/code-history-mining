import analysis.Context
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
import org.jetbrains.annotations.Nullable
import ui.FileAmountToolWindow
import util.CancelledException
import util.Measure

import static com.intellij.openapi.ui.Messages.showWarningDialog
import static com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid.SPEEDSEARCH
import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static liveplugin.PluginUtil.*
import static ui.Dialog.showDialog
import static util.Measure.measure
//noinspection GroovyConstantIfStatement
//if (false) return showFileAmountByType(project)
//noinspection GroovyConstantIfStatement
if (false) return CommitMunging_Playground.playOnIt()

class VcsAccess {

	boolean noVCSRootsIn(Project project) {
		ChangeEventsReader.noVCSRootsIn(project)
	}
}

class Miner {
	private final UI ui
	private final HistoryStorage storage
	private final VcsAccess vcsAccess

	Miner(UI ui, HistoryStorage storage, VcsAccess vcsAccess) {
		this.ui = ui
		this.storage = storage
		this.vcsAccess = vcsAccess
	}

	void createVisualization(File file, Visualization visualization) {
		ui.runInBackground("Creating ${visualization.name.toLowerCase()}") { ProgressIndicator indicator ->
			try {
				Measure.reset()

				def projectName = storage.guessProjectNameFrom(file.name)
				def checkIfCancelled = CancelledException.check(indicator)

				def events = storage.readAllEvents(file.name, checkIfCancelled)
				def context = new Context(events, projectName, checkIfCancelled)
				def html = visualization.generate(context)

				ui.showInBrowser(html, projectName, visualization)

				Measure.forEachDuration{ ui.log_(it) }
			} catch (CancelledException ignored) {
				log_("Cancelled building '${visualization.name}'")
			}
		}
	}

	def fileCountByFileExtension(Project project) {
		def scope = GlobalSearchScope.projectScope(project)
		FileTypeManager.instance.registeredFileTypes.inject([:]) { Map map, FileType fileType ->
			int fileCount = FileBasedIndex.instance.getContainingFiles(FileTypeIndex.NAME, fileType, scope).size()
			if (fileCount > 0) map.put(fileType.defaultExtension, fileCount)
			map
		}.sort{ -it.value }
	}

	def grabHistoryOf(Project project) {
		if (vcsAccess.noVCSRootsIn(project)) {
			return ui.showNoVcsRootsMessage(project)
		}

		ui.showGrabbingDialog(project) { HistoryGrabberConfig userInput ->
			ui.runInBackground("Grabbing project history") { ProgressIndicator indicator ->
				measure("Total time") {
					def eventStorage = new EventStorage(userInput.outputFilePath)
					def vcsRequestBatchSizeInDays = 1 // based on personal observation (hardcoded so that not to clutter UI dialog)
					def eventsReader = new ChangeEventsReader(
							project,
							new CommitReader(project, vcsRequestBatchSizeInDays),
							new CommitFilesMunger(project, userInput.grabChangeSizeInLines).&mungeCommit
					)

					def message = doGrabHistory(eventsReader, eventStorage, userInput, indicator)

					ui.showGrabbingFinishedMessage(message.text, message.title, project)
				}
				Measure.forEachDuration{ ui.log_(it) }
			}
		}
	}

	private static doGrabHistory(ChangeEventsReader eventsReader, EventStorage storage, HistoryGrabberConfig config, indicator = null) {
		def updateIndicatorText = { changeList, callback ->
			log_(changeList.name)
			def date = dateFormat.format((Date) changeList.commitDate)
			indicator?.text = "Grabbing project history (${date} - '${changeList.comment.trim()}')"

			callback()

			indicator?.text = "Grabbing project history (${date} - looking for next commit...)"
		}
		def isCancelled = { indicator?.canceled }

		def fromDate = config.from
		def toDate = config.to + 1 // "+1" add a day to make date in UI inclusive

		def allEventWereStored = true
		def appendToStorage = { commitChangeEvents -> allEventWereStored &= storage.appendToEventsFile(commitChangeEvents) }
		def prependToStorage = { commitChangeEvents -> allEventWereStored &= storage.prependToEventsFile(commitChangeEvents) }

		if (storage.hasNoEvents()) {
			log_("Loading project history from ${fromDate} to ${toDate}")
			eventsReader.readPresentToPast(fromDate, toDate, isCancelled, updateIndicatorText, appendToStorage)

		} else {
			if (toDate > timeAfterMostRecentEventIn(storage)) {
				def (historyStart, historyEnd) = [timeAfterMostRecentEventIn(storage), toDate]
				log_("Loading project history from $historyStart to $historyEnd")
				// read events from past into future because they are prepended to storage
				eventsReader.readPastToPresent(historyStart, historyEnd, isCancelled, updateIndicatorText, prependToStorage)
			}

			if (fromDate < timeBeforeOldestEventIn(storage)) {
				def (historyStart, historyEnd) = [fromDate, timeBeforeOldestEventIn(storage)]
				log_("Loading project history from $historyStart to $historyEnd")
				eventsReader.readPresentToPast(historyStart, historyEnd, isCancelled, updateIndicatorText, appendToStorage)
			}
		}

		def messageText = ""
		if (storage.hasNoEvents()) {
			messageText += "Grabbed history to ${storage.filePath}\n"
			messageText += "However, it has nothing in it probably because there are no commits from $fromDate to $toDate\n"
		} else {
			messageText += "Grabbed history to ${storage.filePath}\n"
			messageText += "It should have history from '${storage.oldestEventTime}' to '${storage.mostRecentEventTime}'.\n"
		}
		if (eventsReader.lastRequestHadErrors) {
			messageText += "\nThere were errors while reading commits from VCS, please check IDE log for details.\n"
		}
		if (!allEventWereStored) {
			messageText += "\nSome of events were not added to csv file because it already contained events within the time range\n"
		}
		[text: messageText, title: "Code History Mining"]
	}

	private static timeBeforeOldestEventIn(EventStorage storage) {
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

	private static timeAfterMostRecentEventIn(EventStorage storage) {
		def date = storage.mostRecentEventTime
		if (date == null) {
			new Date()
		} else {
			date.time += 1000  // plus one second (see comments in timeBeforeOldestEventIn())
			date
		}
	}

	static log_(String message) { Logger.getInstance("CodeHistoryMining").info(message) }

}


class HistoryStorage {
	private final String basePath

	HistoryStorage(String basePath) {
		this.basePath = basePath
	}

	File[] filesWithCodeHistory() {
		new File(basePath).listFiles(new FileFilter() {
			@Override boolean accept(File pathName) { pathName.name.endsWith(".csv") }
		})
	}

	HistoryGrabberConfig loadGrabberConfigFor(Project project) {
		HistoryGrabberConfig.loadGrabberConfigFor(project, basePath) {
			def outputFilePath = "${basePath}/${project.name + "-file-events.csv"}"
			new HistoryGrabberConfig(new Date() - 300, new Date(), outputFilePath, false)
		}
	}

	def saveGrabberConfigFor(Project project, HistoryGrabberConfig config) {
		HistoryGrabberConfig.saveGrabberConfigOf(project, basePath, config)
	}

	boolean isValidName(String fileName) {
		fileName.length() > 0 && !new File("$basePath/$fileName").exists()
	}

	def rename(String fileName, String newFileName) {
		FileUtil.rename(new File("$basePath/$fileName"), new File("$basePath/$newFileName"))
	}

	def delete(String fileName) {
		FileUtil.delete(new File("$basePath/$fileName"))
	}

	def readAllEvents(String fileName, Closure<Void> checkIfCancelled) {
		measure("Storage.readAllEvents"){
			new EventStorage("$basePath/$fileName").readAllEvents(checkIfCancelled){ line, e -> UI.log_("Failed to parse line '${line}'") }
		}
	}

	String guessProjectNameFrom(String fileName) {
		fileName.replace(".csv", "").replace("-file-events", "")
	}
}


class UI {
	Miner miner
	HistoryStorage storage

	UI() {
		def actionGroup = new ActionGroup("Code History Mining", true) {
			@Override AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
				def codeHistoryActions = storage.filesWithCodeHistory().collect{ createActionsOnHistoryFile(it) }
				[grabHistory(), Separator.instance] + codeHistoryActions + [Separator.instance, projectStats(), openReadme()]
			}
		}
		registerAction("CodeHistoryMiningMenu", "", "VcsGroups", "Code History Mining", actionGroup)
		registerAction("CodeHistoryMiningPopup", "alt shift H", "", "Show Code History Mining Popup") { AnActionEvent actionEvent ->
			JBPopupFactory.instance.createActionGroupPopup(
					"Code History Mining",
					actionGroup,
					actionEvent.dataContext,
					SPEEDSEARCH,
					true
			).showCenteredInCurrentWindow(actionEvent.project)
		}
	}

	def showGrabbingDialog(Project project, Closure onOkCallback) {
		def grabberConfig = storage.loadGrabberConfigFor(project)
		showDialog(grabberConfig, "Grab History Of Current Project", project) { HistoryGrabberConfig userInput ->
			storage.saveGrabberConfigFor(project, userInput)
			onOkCallback.call(userInput)
		}
	}

	def showInBrowser(String html, String projectName, Visualization visualization) {
		def url = HttpUtil.loadIntoHttpServer(html, projectName, visualization.name + ".html")

		// need to check if browser configured correctly because it looks like IntelliJ won't do it
		def browserConfiguredCorrectly = new File(GeneralSettings.instance.browserPath).exists()
		if (!browserConfiguredCorrectly) {
			UIUtil.invokeLaterIfNeeded{
				showWarningDialog(
						"It seems that browser is not configured correctly.\nPlease check Settings -> Web Browsers config.",
						"Code History Mining"
				)
			}
			// don't return and try to open url anyway in case the above check is wrong
		}
		BrowserUtil.launchBrowser(url)
	}

	def showNoVcsRootsMessage(Project project) {
		showWarningDialog(project, "Cannot grab project history because there are no VCS roots setup for it.", "Code History Mining")
	}

	def showGrabbingFinishedMessage(String message, String title, Project project) {
		showInConsole(message, title, project)
	}

	def runInBackground(String taskDescription, Closure closure) {
		doInBackground(taskDescription, closure)
	}

	static def log_(String message) { // TODO
		Logger.getInstance("CodeHistoryMining").info(message)
	}

	private grabHistory() {
		registerAction("GrabProjectHistory", "", "", "Grab Project History"){ AnActionEvent event ->
			miner.grabHistoryOf(event.project)
		}
	}

	private projectStats() {
		new AnAction("Amount of Files in Project") {
			@Override void actionPerformed(AnActionEvent event) {
				FileAmountToolWindow.showIn(event.project, UI.this.miner.fileCountByFileExtension(event.project))
			}
		}
	}

	private static openReadme() {
		new AnAction("Read Me (page on GitHub)") {
			@Override void actionPerformed(AnActionEvent event) {
				BrowserUtil.open("https://github.com/dkandalov/code-history-mining#how-to-use")
			}
		}
	}

	private AnAction createActionsOnHistoryFile(File file) {
		Closure<AnAction> createShowInBrowserAction = { Visualization visualization ->
			new AnAction(visualization.name) {
				@Override void actionPerformed(AnActionEvent event) {
					miner.createVisualization(file, visualization)
				}
			}
		}
		new DefaultActionGroup(file.name, true).with {
			add(createShowInBrowserAction(Visualization.all))
			add(createShowInBrowserAction(Visualization.commitLogAsGraph))
			add(Separator.instance)
			add(createShowInBrowserAction(Visualization.changeSizeChart))
			add(createShowInBrowserAction(Visualization.amountOfCommittersChart))
			add(createShowInBrowserAction(Visualization.amountOfFilesInCommitChart))
			add(createShowInBrowserAction(Visualization.amountOfCommitsTreemap))
			add(createShowInBrowserAction(Visualization.filesInTheSameCommitGraph))
			add(createShowInBrowserAction(Visualization.committersChangingSameFilesGraph))
			add(createShowInBrowserAction(Visualization.commitTimePunchcard))
			add(createShowInBrowserAction(Visualization.timeBetweenCommitsHistogram))
			add(createShowInBrowserAction(Visualization.commitMessageWordCloud))
			add(Separator.instance)
			add(showInFileManager(file))
			add(rename(file.name))
			add(delete(file.name))
			it
		}
	}

	private static showInFileManager(File file) {
		new AnAction("Show in File Manager") {
			@Override void actionPerformed(AnActionEvent event) {
				ShowFilePathAction.openFile(file)
			}
		}
	}

	private rename(String fileName) {
		new AnAction("Rename") {
			@Override void actionPerformed(AnActionEvent event) {
				def newFileName = Messages.showInputDialog("New file name:", "Rename File", null, fileName, new InputValidator() {
					@Override boolean checkInput(String newFileName) { UI.this.storage.isValidName(newFileName) }
					@Override boolean canClose(String newFileName) { true }
				})
				if (newFileName != null) UI.this.storage.rename(fileName, newFileName)
			}
		}
	}

	private delete(String fileName) {
		new AnAction("Delete") {
			@Override void actionPerformed(AnActionEvent event) {
				int userAnswer = Messages.showOkCancelDialog("Delete ${fileName}?", "Delete File", "&Delete", "&Cancel", UIUtil.getQuestionIcon())
				if (userAnswer == Messages.OK) storage.delete(fileName)
			}
		}
	}
}


def pathToHistoryFiles = "${PathManager.pluginsPath}/code-history-mining"

def storage = new HistoryStorage(pathToHistoryFiles)
def vcsAccess = new VcsAccess()
def ui = new UI()
def miner = new Miner(ui, storage, vcsAccess)
ui.miner = miner
ui.storage = storage


if (!isIdeStartup) show("Reloaded code-history-mining plugin")


