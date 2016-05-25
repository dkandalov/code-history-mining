package codehistoryminer.plugin.ui

import codehistoryminer.core.miner.Data
import codehistoryminer.core.miner.DataWrapper
import codehistoryminer.core.visualizations.Visualization
import codehistoryminer.core.visualizations.VisualizedAnalyzer
import codehistoryminer.plugin.CodeHistoryMinerPlugin
import codehistoryminer.plugin.historystorage.HistoryGrabberConfig
import codehistoryminer.plugin.historystorage.HistoryStorage
import codehistoryminer.plugin.ui.http.HttpUtil
import codehistoryminer.publicapi.analysis.values.Table
import codehistoryminer.publicapi.analysis.values.TableList
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.GeneralSettings
import com.intellij.ide.actions.ShowFilePathAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.application.ApplicationManager
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
import liveplugin.CanCallFromAnyThread
import liveplugin.PluginUtil
import liveplugin.implementation.Misc
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable

import javax.swing.event.HyperlinkEvent

import static codehistoryminer.core.visualizations.VisualizedAnalyzer.Bundle.*
import static codehistoryminer.plugin.ui.templates.PluginTemplates.pluginTemplate
import static com.intellij.execution.ui.ConsoleViewContentType.ERROR_OUTPUT
import static com.intellij.notification.NotificationType.INFORMATION
import static com.intellij.notification.NotificationType.WARNING
import static liveplugin.PluginUtil.registerAction

@SuppressWarnings("GrMethodMayBeStatic")
class UI {
	private CodeHistoryMinerPlugin minerPlugin
	private HistoryStorage historyStorage
	private Log log
	private ProjectManagerAdapter listener

	def init(CodeHistoryMinerPlugin minerPlugin, HistoryStorage historyStorage, Log log) {
		this.minerPlugin = minerPlugin
		this.historyStorage = historyStorage
		this.log = log

		def grabHistory = grabHistory()
		def projectStats = projectStats()
		def currentFileHistoryStats = currentFileHistoryStats()
		def openReadme = openReadme()

		def actionGroup = new ActionGroup("Code History Mining", true) {
			@Override AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
				def codeHistoryActions = historyStorage.filesWithCodeHistory().collect{ createActionsOnHistoryFile(it) }
				[grabHistory, Separator.instance] + codeHistoryActions +
				[Separator.instance, currentFileHistoryStats, projectStats, openReadme]
			}
		}
		registerAction("CodeHistoryMiningMenu", "", "VcsGroups", "Code History Mining", actionGroup)
		registerAction("CodeHistoryMiningPopup", "alt shift H", "", "Show Code History Mining Popup") { AnActionEvent actionEvent ->
			JBPopupFactory.instance.createActionGroupPopup(
					"Code History Mining",
					actionGroup,
					actionEvent.dataContext,
					JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
					true
			).showCenteredInCurrentWindow(actionEvent.project)
		}
		registerAction("CodeHistoryMiningRunScript", "alt shift E", "EditorPopupMenu", "Run Code History Script", runScriptAction())

		listener = new ProjectManagerAdapter() {
			@Override void projectOpened(Project project) { minerPlugin.onProjectOpened(project) }
			@Override void projectClosed(Project project) { minerPlugin.onProjectClosed(project) }
		}
		ProjectManager.instance.addProjectManagerListener(listener)
		ProjectManager.instance.openProjects.each{ minerPlugin.onProjectOpened(it) }
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

	def showInBrowser(String html, String projectName, String visualizationName) {
		def url = HttpUtil.loadIntoHttpServer(html, projectName, visualizationName + ".html", log)

		// need to check if browser configured correctly because it looks like IntelliJ won't do it
		if (browserConfiguredIncorrectly()) {
			PluginUtil.invokeLaterOnEDT{
				Messages.showWarningDialog(
						"It seems that browser is not configured correctly.\nPlease check Settings -> Web Browsers config.",
						"Code History Mining"
				)
			}
			// don't return and try to open url anyway in case the above check is wrong
		}
		BrowserUtil.browse(url)
	}

	def openFileInIdeEditor(File file, Project project) {
		PluginUtil.invokeLaterOnEDT {
			def virtualFile = PluginUtil.openInEditor(file.absolutePath, project)
			if (virtualFile == null) show("Didn't find ${"file://" + file.absolutePath}", "", WARNING)
		}
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

	def showGrabbingFinishedMessage(String message, Project project) {
		UIUtil.invokeLaterIfNeeded{
			show(message, "Code History Mining", INFORMATION, "Code History Mining", new NotificationListener() {
				@Override void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
					def linkUrl = event.URL.path
					if (linkUrl.endsWith("/visualize")) {
						linkUrl = linkUrl.replace("/visualize", "")
						minerPlugin.runAnalyzer(new File(linkUrl), project, all, all.name())
					} else {
						openInIde(new File(linkUrl), project)
					}
				}
			})
		}
	}

	def runInBackground(String taskDescription, Closure closure) {
		PluginUtil.doInBackground(taskDescription, closure)
	}

	def showFileHistoryStatsToolWindow(Project project, Map statsMap) {
		PluginUtil.invokeOnEDT {
			FileHistoryStatsToolWindow.showIn(project, statsMap)
		}
	}

	def showFileHasNoVcsHistory(VirtualFile virtualFile) {
		PluginUtil.show("File ${virtualFile.name} has no VCS history")
	}

	def failedToLoadAnalyzers(String scriptFilePath) {
		PluginUtil.show("Failed to load analyzers from '$scriptFilePath'", "", WARNING)
	}

	def showNoHistoryForScript(String scriptFileName) {
		PluginUtil.show("No history file was found for '$scriptFileName' script")
	}

	def showScriptError(String scriptFileName, String message, Project project) {
		PluginUtil.showInConsole(message, scriptFileName, project, ERROR_OUTPUT)
	}

	def showAnalyzerError(String analyzerName, String message, Project project) {
		PluginUtil.showInConsole(message, analyzerName, project, ERROR_OUTPUT)
	}

	def showAnalyzerResult(result, String projectName, Project project) {
		if (result instanceof Visualization) {
			showAnalyzerResult([result], projectName, project)

		} else if (result instanceof Table) {
			showAnalyzerResult([result], projectName, project)

		} else if (result instanceof TableList) {
			showAnalyzerResult(result.tables, projectName, project)

		} else if (result instanceof Collection && !result.empty) {
			def first = result.first()
			if (first instanceof DataWrapper) {
				result = result.collect{ it.data }
				first = result.first()
			}
			if (first instanceof Map || first instanceof Data) {
				openFileInIdeEditor(AnalyzerResultHandlers.saveDataCollectionAsCsvFile(result, projectName), project)

			} else if (first instanceof Visualization) {
				int i = 0
				def template = result.inject(pluginTemplate) { accTemplate, it ->
					it.template.fill("id", "\"id${i++}\"").pasteInto(accTemplate)
				}
				def html = template.fillProjectName(projectName).inlineImports().text
				showInBrowser(html, projectName, projectName)

			} else if (first instanceof Table) {
				AnalyzerResultHandlers.saveTablesAsCsvFile(result, projectName).each { file ->
					openFileInIdeEditor(file, project)
				}

			} else {
				result.each { showAnalyzerResult(it, projectName, project) }
			}
		} else if (result instanceof File) {
			openFileInIdeEditor(result, project)
		} else {
			PluginUtil.show(result)
		}
	}

	private static boolean browserConfiguredIncorrectly() {
		def settings = GeneralSettings.instance
		!settings.useDefaultBrowser && !new File(settings.browserPath).exists()
	}

	private grabHistory() {
		registerAction("GrabProjectHistory", "", "", "Grab Project History"){ AnActionEvent event ->
			minerPlugin.grabHistoryOf(event.project)
		}
	}

	private projectStats() {
		new AnAction("Amount of Files in Project") {
			@Override void actionPerformed(AnActionEvent event) {
				FileAmountToolWindow.showIn(event.project, UI.this.minerPlugin.fileCountByFileExtension(event.project))
			}
		}
	}

	private AnAction createActionsOnHistoryFile(File file) {
		Closure<AnAction> showVisualizationAction = { VisualizedAnalyzer analyzer ->
			new AnAction(analyzer.name()) {
				@Override void actionPerformed(AnActionEvent event) {
					minerPlugin.runAnalyzer(file, event.project, analyzer, analyzer.name())
				}
			}
		}
		new DefaultActionGroup(file.name, true).with {
			add(showVisualizationAction(all))
			add(openScriptEditorAction(file))
			add(Separator.instance)
			add(showVisualizationAction(codeChurnChart))
			add(showVisualizationAction(amountOfCommittersChart))
			add(showVisualizationAction(commitsByCommitterChart))
			add(showVisualizationAction(amountOfTodosChart))
			add(showVisualizationAction(amountOfFilesInCommitChart))
			add(showVisualizationAction(amountOfChangingFilesChart))
			add(showVisualizationAction(changeSizeByFileTypeChart))
			add(showVisualizationAction(changesTreemap))
			add(showVisualizationAction(filesInTheSameCommitGraph))
			add(showVisualizationAction(committersChangingSameFilesGraph))
			add(showVisualizationAction(commitTimePunchcard))
			add(showVisualizationAction(commitMessagesWordChart))
			add(showVisualizationAction(commitMessageWordCloud))
			add(Separator.instance)
			add(showVisualizationAction(commitLogAsGraph))
			add(Separator.instance)
			add(showInFileManager(file))
			add(openInIdeAction(file))
			add(renameFileAction(file.name))
			add(deleteFileAction(file.name))
			it
		}
	}

	private currentFileHistoryStats() {
		new AnAction("Current File History Stats") {
			@Override void actionPerformed(AnActionEvent event) {
				UI.this.minerPlugin.showCurrentFileHistoryStats(event.project)
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

	private static openInIdeAction(File file) {
		new AnAction("Open in IDE") {
			@Override void actionPerformed(AnActionEvent event) {
				openInIde(file, event.project)
			}
		}
	}

	private static openInIde(File file, Project project) {
		def virtualFile = VirtualFileManager.instance.refreshAndFindFileByUrl("file://" + file.canonicalPath)
		if (virtualFile != null) {
			FileEditorManager.getInstance(project).openFile(virtualFile, true)
		} else {
			show("Couldn't find file ${file.canonicalPath} to open in IDE", "", WARNING)
		}
	}

	private static showInFileManager(File file) {
		new AnAction("Show in File Manager") {
			@Override void actionPerformed(AnActionEvent event) {
				ShowFilePathAction.openFile(file)
			}
		}
	}

	private renameFileAction(String fileName) {
		new AnAction("Rename") {
			@Override void actionPerformed(AnActionEvent event) {
				def newFileName = Messages.showInputDialog("New file name:", "Rename File", null, fileName, new InputValidator() {
					@Override boolean checkInput(String newFileName) { UI.this.historyStorage.isValidNewFileName(newFileName) }
					@Override boolean canClose(String newFileName) { true }
				})
				if (newFileName != null) UI.this.historyStorage.rename(fileName, newFileName)
			}
		}
	}

	private deleteFileAction(String fileName) {
		new AnAction("Delete") {
			@Override void actionPerformed(AnActionEvent event) {
				int userAnswer = Messages.showOkCancelDialog("Delete ${fileName}?", "Delete File", "&Delete", "&Cancel", UIUtil.getQuestionIcon())
				if (userAnswer == Messages.OK) historyStorage.delete(fileName)
			}
		}
	}

	private openScriptEditorAction(File file) {
		new AnAction("Open Script Editor") {
			@Override void actionPerformed(AnActionEvent event) {
				minerPlugin.openScriptEditorFor(event.project, file)
			}
		}
	}

	private runScriptAction() {
		new AnAction(AllIcons.Actions.Execute) {
			@Override void actionPerformed(AnActionEvent event) {
				minerPlugin.runCurrentFileAsScript(event.project)
			}
			@Override void update(AnActionEvent event) {
				def isScript = minerPlugin.isCurrentFileScript(event.project)
				event.presentation.enabled = isScript
				event.presentation.visible = isScript
			}
		}
	}

	@CanCallFromAnyThread
	static show(@Nullable message, @Nullable String title = "", NotificationType notificationType = INFORMATION,
	            String groupDisplayId = "", @Nullable NotificationListener notificationListener = null) {
		PluginUtil.invokeLaterOnEDT {
			message = Misc.asString(message)
			// this is because Notification doesn't accept empty messages
			if (message.trim().empty) message = "[empty message]"

			def notification = new Notification(groupDisplayId, title, message, notificationType, notificationListener)
			ApplicationManager.application.messageBus.syncPublisher(Notifications.TOPIC).notify(notification)
		}
	}

	interface Log {
		def httpServerIsAboutToLoadHtmlFile(String fileName)
		def errorOnHttpRequest(String message)
	}
}
