package ui

import analysis.Visualization
import com.intellij.ide.BrowserUtil
import com.intellij.ide.GeneralSettings
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.util.ui.UIUtil
import historyreader.HistoryGrabberConfig
import historyreader.HistoryStorage
import http.HttpUtil
import liveplugin.PluginUtil
import miner.Miner
import org.jetbrains.annotations.Nullable

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
		PluginUtil.registerAction("CodeHistoryMiningMenu", "", "VcsGroups", "Code History Mining", actionGroup)
		PluginUtil.registerAction("CodeHistoryMiningPopup", "alt shift H", "", "Show Code History Mining Popup") { AnActionEvent actionEvent ->
			JBPopupFactory.instance.createActionGroupPopup(
					"Code History Mining",
					actionGroup,
					actionEvent.dataContext,
					JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
					true
			).showCenteredInCurrentWindow(actionEvent.project)
		}
	}

	def showGrabbingDialog(Project project, Closure onOkCallback) {
		def grabberConfig = storage.loadGrabberConfigFor(project)
		Dialog.showDialog(grabberConfig, "Grab History Of Current Project", project) { HistoryGrabberConfig userInput ->
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
				Messages.showWarningDialog(
						"It seems that browser is not configured correctly.\nPlease check Settings -> Web Browsers config.",
						"Code History Mining"
				)
			}
			// don't return and try to open url anyway in case the above check is wrong
		}
		BrowserUtil.launchBrowser(url)
	}

	def showNoVcsRootsMessage(Project project) {
		Messages.showWarningDialog(project, "Cannot grab project history because there are no VCS roots setup for it.", "Code History Mining")
	}

	def showGrabbingFinishedMessage(String message, String title, Project project) {
		PluginUtil.showInConsole(message, title, project)
	}

	def runInBackground(String taskDescription, Closure closure) {
		PluginUtil.doInBackground(taskDescription, closure)
	}

	static def log_(String message) { // TODO
		Logger.getInstance("CodeHistoryMining").info(message)
	}

	private grabHistory() {
		PluginUtil.registerAction("GrabProjectHistory", "", "", "Grab Project History"){ AnActionEvent event ->
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
