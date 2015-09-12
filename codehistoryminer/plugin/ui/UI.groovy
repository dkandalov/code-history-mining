package codehistoryminer.plugin.ui
import codehistoryminer.core.visualizations.Visualization
import codehistoryminer.historystorage.HistoryGrabberConfig
import codehistoryminer.historystorage.HistoryStorage
import codehistoryminer.plugin.CodeHistoryMinerPlugin
import codehistoryminer.plugin.ui.http.HttpUtil
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
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.ui.UIUtil
import liveplugin.PluginUtil
import org.jetbrains.annotations.Nullable

import static com.intellij.execution.ui.ConsoleViewContentType.ERROR_OUTPUT

@SuppressWarnings("GrMethodMayBeStatic")
class UI {
	CodeHistoryMinerPlugin miner
	HistoryStorage storage
	Log log
	private ProjectManagerAdapter listener

	def init() {
		def grabHistory = grabHistory()
		def projectStats = projectStats()
		def currentFileHistoryStats = currentFileHistoryStats()
		def openReadme = openReadme()

		def actionGroup = new ActionGroup("Code History Mining", true) {
			@Override AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
				def codeHistoryActions = storage.filesWithCodeHistory().collect{ createActionsOnHistoryFile(it) }
				[grabHistory, Separator.instance] + codeHistoryActions +
				[Separator.instance, currentFileHistoryStats, projectStats, openReadme]
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
		PluginUtil.registerAction("CodeHistoryMiningRunQuery", "alt C, alt Q", "", "Run Code History Query") { AnActionEvent event ->
			miner.runCurrentFileAsHistoryQueryScript(event.project)
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
		GrabHistoryDialog.showDialog(grabberConfig, "Grab History Of Current Project", project, onApplyCallback) { HistoryGrabberConfig userInput ->
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

	def openFileInIdeEditor(File file, Project project) {
		PluginUtil.openInEditor(file.absolutePath, project)
	}

	def showGrabbingInProgressMessage(Project project) {
		UIUtil.invokeLaterIfNeeded{
			Messages.showInfoMessage(project, "Grabbing project history is already in progress. Please wait for it to finish or cancel it.", "Code History Mining")
		}
	}

	def showNoVcsRootsMessage(Project project) {
		UIUtil.invokeLaterIfNeeded{
			Messages.showWarningDialog(project, "Cannot grab project history because there are no VCS roots setup for it.", "Code History Mining")
		}
	}

	def showNoEventsInStorageMessage(String fileName, Project project) {
		UIUtil.invokeLaterIfNeeded{
			Messages.showInfoMessage(project, "There is no data in ${fileName} so nothing to visualize.", "Code History Mining")
		}
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

	def showFileHistoryStatsToolWindow(Project project, Map statsMap) {
		PluginUtil.invokeOnEDT {
			FileHistoryStatsToolWindow.showIn(project, statsMap)
		}
	}

	def showFileHasNoVcsHistory(VirtualFile virtualFile) {
		PluginUtil.show("File ${virtualFile.name} has no VCS history")
	}

	def showResultOfQueryScript(String scriptFileName, Object result) {
		PluginUtil.show("Executed '$scriptFileName'")
		PluginUtil.show(result)
	}

	def showNoHistoryForQueryScript(String scriptFileName) {
		PluginUtil.show("No history file was found for '$scriptFileName' query script")
	}

	def showQueryScriptError(String scriptFileName, String message, Project project) {
		PluginUtil.showInConsole(message, scriptFileName, project, ERROR_OUTPUT)
	}

	private currentFileHistoryStats() {
		new AnAction("Current File History Stats") {
			@Override void actionPerformed(AnActionEvent event) {
				UI.this.miner.showCurrentFileHistoryStats(event.project)
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
		Closure<AnAction> createRunQueryAction = {
			new AnAction("Open Query Editor") {
				@Override void actionPerformed(AnActionEvent event) {
					miner.openQueryEditorFor(event.project, file)
				}
			}
		}
		Closure<AnAction> createShowVisualizationAction = { Visualization visualization ->
			new AnAction(visualization.name) {
				@Override void actionPerformed(AnActionEvent event) {
					miner.createVisualization(file, visualization, event.project)
				}
			}
		}
		new DefaultActionGroup(file.name, true).with {
			add(createShowVisualizationAction(Visualization.all))
			add(createShowVisualizationAction(Visualization.commitLogAsGraph))
			add(Separator.instance)
			add(createShowVisualizationAction(Visualization.codeChurnChart))
			add(createShowVisualizationAction(Visualization.amountOfCommittersChart))
			add(createShowVisualizationAction(Visualization.amountOfCommitsByCommitter))
			add(createShowVisualizationAction(Visualization.amountOfTodosChart))
			add(createShowVisualizationAction(Visualization.amountOfFilesInCommitChart))
			add(createShowVisualizationAction(Visualization.amountOfChangingFilesChart))
			add(createShowVisualizationAction(Visualization.changeSizeByFileTypeChart))
			add(createShowVisualizationAction(Visualization.changesTreemap))
			add(createShowVisualizationAction(Visualization.filesInTheSameCommitGraph))
			add(createShowVisualizationAction(Visualization.committersChangingSameFilesGraph))
			add(createShowVisualizationAction(Visualization.commitTimePunchcard))
			add(createShowVisualizationAction(Visualization.timeBetweenCommitsHistogram))
			add(createShowVisualizationAction(Visualization.commitMessagesWordChart))
			add(createShowVisualizationAction(Visualization.commitMessageWordCloud))
			add(Separator.instance)
			add(createRunQueryAction())
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
					@Override boolean checkInput(String newFileName) { UI.this.storage.isValidNewFileName(newFileName) }
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
