package codemining.plugin.ui
import codemining.core.visualizations.Visualization
import codemining.historystorage.HistoryGrabberConfig
import codemining.historystorage.HistoryStorage
import codemining.plugin.CodeMiningPlugin
import codemining.plugin.ui.http.HttpUtil
import com.intellij.ide.BrowserUtil
import com.intellij.ide.GeneralSettings
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerAdapter
import com.intellij.openapi.ui.InputValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.ui.UIUtil
import liveplugin.PluginUtil
import org.jetbrains.annotations.Nullable

@SuppressWarnings("GrMethodMayBeStatic")
class UI {
	CodeMiningPlugin miner
	HistoryStorage storage
	Log log
	private ProjectManagerAdapter listener

	def init() {
		def grabHistory = grabHistory()
		def projectStats = projectStats()
		def openReadme = openReadme()

		def actionGroup = new ActionGroup("Code History Mining", true) {
			@Override AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
				def codeHistoryActions = storage.filesWithCodeHistory().collect{ createActionsOnHistoryFile(it) }
				[grabHistory, Separator.instance] + codeHistoryActions + [Separator.instance, projectStats, openReadme]
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

		listener = new ProjectManagerAdapter() {
			@Override void projectOpened(Project project) { miner.onProjectOpened(project) }
			@Override void projectClosed(Project project) { miner.onProjectClosed(project) }
		}
		ProjectManager.instance.addProjectManagerListener(listener)
		ProjectManager.instance.openProjects.each{ miner.onProjectOpened(it) }
	}

	def dispose(oldUI) {
		def oldListener = oldUI?.listener
		if (oldListener != null) {
			ProjectManager.instance.removeProjectManagerListener(oldListener)
		}
	}

	def showGrabbingDialog(HistoryGrabberConfig grabberConfig, Project project, Closure onApplyCallback, Closure onGrabCallback) {
		Dialog.showDialog(grabberConfig, "Grab History Of Current Project", project, onApplyCallback) { HistoryGrabberConfig userInput ->
			onGrabCallback.call(userInput)
		}
	}

	def showInBrowser(String html, String projectName, Visualization visualization) {
		def url = HttpUtil.loadIntoHttpServer(html, projectName, visualization.name + ".html", log)

		// need to check if browser configured correctly because it looks like IntelliJ won't do it
		if (browserConfiguredIncorrectly()) {
			UIUtil.invokeLaterIfNeeded{
				Messages.showWarningDialog(
						"It seems that browser is not configured correctly.\nPlease check Settings -> Web Browsers config.",
						"Code History Mining"
				)
			}
			// don't return and try to open url anyway in case the above check is wrong
		}
		BrowserUtil.browse(url)
	}

	private static boolean browserConfiguredIncorrectly() {
		def settings = GeneralSettings.instance
		!settings.useDefaultBrowser && !new File(settings.browserPath).exists()
	}

	def showGrabbingInProgressMessage(Project project) {
		Messages.showInfoMessage(project, "Grabbing project history is already in progress. Please wait for it to finish or cancel it.", "Code History Mining")
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
			add(createShowInBrowserAction(Visualization.amountOfTodosChart))
			add(createShowInBrowserAction(Visualization.amountOfFilesInCommitChart))
			add(createShowInBrowserAction(Visualization.amountOfChangingFilesChart))
			add(createShowInBrowserAction(Visualization.changeSizeByFileTypeChart))
			add(createShowInBrowserAction(Visualization.amountOfCommitsTreemap))
			add(createShowInBrowserAction(Visualization.filesInTheSameCommitGraph))
			add(createShowInBrowserAction(Visualization.committersChangingSameFilesGraph))
			add(createShowInBrowserAction(Visualization.commitTimePunchcard))
			add(createShowInBrowserAction(Visualization.timeBetweenCommitsHistogram))
			add(createShowInBrowserAction(Visualization.wordOccurrencesChart))
			add(createShowInBrowserAction(Visualization.commitMessageWordCloud))
			add(Separator.instance)
			add(showInFileManager(file))
			add(openInIde(file))
			add(rename(file.name))
			add(delete(file.name))
			it
		}
	}

	private static openInIde(File file) {
		new AnAction("Open in IDE") {
			@Override void actionPerformed(AnActionEvent event) {
				def virtualFile = VirtualFileManager.instance.findFileByUrl("file://" + file.canonicalPath)
				if (virtualFile != null) {
					FileEditorManager.getInstance(event.project).openFile(virtualFile, true)
				}
			}
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


	interface Log {
		def httpServerIsAboutToLoadHtmlFile(String fileName)
		def errorOnHttpRequest(String message)
	}
}
