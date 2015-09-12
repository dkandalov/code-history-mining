package codehistoryminer.plugin
import codehistoryminer.core.common.langutil.*
import codehistoryminer.core.historystorage.EventStorage2
import codehistoryminer.core.vcs.miner.MinedCommit
import codehistoryminer.core.visualizations.Visualization
import codehistoryminer.core.visualizations.VisualizationListener
import codehistoryminer.historystorage.HistoryGrabberConfig
import codehistoryminer.historystorage.HistoryStorage
import codehistoryminer.historystorage.QueryScriptsStorage
import codehistoryminer.plugin.ui.UI
import codehistoryminer.vcsaccess.VcsActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import groovy.time.TimeCategory
import liveplugin.PluginUtil

import static codehistoryminer.core.common.langutil.Date.Formatter.dd_MM_yyyy
import static codehistoryminer.plugin.ui.templates.PluginTemplates.pluginTemplate

class CodeHistoryMinerPlugin {
	private final UI ui
	private final HistoryStorage historyStorage
	private final QueryScriptsStorage scriptsStorage
	private final VcsActions vcsAccess
	private final Measure measure
	private final CodeHistoryMinerPluginLog log
	private volatile boolean grabHistoryIsInProgress

	CodeHistoryMinerPlugin(UI ui, HistoryStorage historyStorage, QueryScriptsStorage scriptsStorage,
	                       VcsActions vcsAccess, Measure measure, CodeHistoryMinerPluginLog log = null) {
		this.ui = ui
		this.historyStorage = historyStorage
		this.scriptsStorage = scriptsStorage
		this.vcsAccess = vcsAccess
		this.measure = measure
		this.log = log
	}

	def createVisualization(File file, Visualization visualization, Project project) {
		ui.runInBackground("Creating ${visualization.name.toLowerCase()}") { ProgressIndicator indicator ->
			try {
				measure.start()

				def projectName = historyStorage.guessProjectNameFrom(file.name)
				def cancelled = new Cancelled() {
					@Override boolean isTrue() { indicator.canceled }
				}

				def events = historyStorage.readAllEvents(file.name, cancelled)
				if (events.empty) {
					return ui.showNoEventsInStorageMessage(file.name, project)
				}

				def listener = new VisualizationListener() {
					@Override void onProgress(Progress progress) { indicator.fraction = progress.percentComplete() }
					@Override void onLog(String message) { Logger.getInstance("CodeHistoryMining").info(message) }
				}
				def html = visualization
						.generateFrom(events, projectName, cancelled, listener)
						.pasteInto(pluginTemplate)
						.fillProjectName(projectName)
						.inlineImports()
						.text

				ui.showInBrowser(html, projectName, visualization)

				measure.forEachDuration{ log?.measuredDuration(it) }
			} catch (Cancelled ignored) {
				log?.cancelledBuilding(visualization.name)
			}
		}
	}

	@SuppressWarnings("GrMethodMayBeStatic")
	def fileCountByFileExtension(Project project) {
		def scope = GlobalSearchScope.projectScope(project)
		FileTypeManager.instance.registeredFileTypes.inject([:]) { LinkedHashMap map, FileType fileType ->
			int fileCount = FileBasedIndex.instance.getContainingFiles(FileTypeIndex.NAME, fileType, scope).size()
			if (fileCount > 0) map.put(fileType.defaultExtension, fileCount)
			map
		}.sort{ -it.value }
	}

	def onProjectOpened(Project project) {
		def grabberConfig = historyStorage.loadGrabberConfigFor(project.name)
		if (grabberConfig.grabOnVcsUpdate)
			vcsAccess.addVcsUpdateListenerFor(project.name, this.&grabHistoryOnVcsUpdate)
	}

	def onProjectClosed(Project project) {
		vcsAccess.removeVcsUpdateListenerFor(project.name)
	}

	def grabHistoryOf(Project project) {
		if (grabHistoryIsInProgress) return ui.showGrabbingInProgressMessage(project)
		if (vcsAccess.noVCSRootsIn(project)) return ui.showNoVcsRootsMessage(project)

		def saveConfig = { HistoryGrabberConfig userInput ->
			historyStorage.saveGrabberConfigFor(project.name, userInput)
		}

		def grabberConfig = historyStorage.loadGrabberConfigFor(project.name)
		ui.showGrabbingDialog(grabberConfig, project, saveConfig) { HistoryGrabberConfig userInput ->
			saveConfig(userInput)

			if (userInput.grabOnVcsUpdate)
				vcsAccess.addVcsUpdateListenerFor(project.name, this.&grabHistoryOnVcsUpdate)
			else
				vcsAccess.removeVcsUpdateListenerFor(project.name)

			grabHistoryIsInProgress = true
			ui.runInBackground("Grabbing project history") { ProgressIndicator indicator ->
				try {
					measure.start()
					measure.measure("Total time") {
						def eventStorage = historyStorage.eventStorageFor(userInput.outputFilePath)
                        def requestDateRange = new DateRange(userInput.from, userInput.to)

                        def message = doGrabHistory(project, eventStorage, requestDateRange, userInput.grabChangeSizeInLines, indicator)

						ui.showGrabbingFinishedMessage(message.text, message.title, project)
					}
					measure.forEachDuration{ log?.measuredDuration(it) }
				} finally {
					grabHistoryIsInProgress = false
				}
			}
		}
	}

	def grabHistoryOnVcsUpdate(Project project, Time now = Time.now()) {
		if (grabHistoryIsInProgress) return
		def config = historyStorage.loadGrabberConfigFor(project.name)
		now = now.withTimeZone(config.lastGrabTime.timeZone())
		if (config.lastGrabTime.floorToDay() == now.floorToDay()) return

		grabHistoryIsInProgress = true
		ui.runInBackground("Grabbing project history") { ProgressIndicator indicator ->
			try {
				def eventStorage = historyStorage.eventStorageFor(config.outputFilePath)
				def fromDate = eventStorage.storedDateRange().to
				def toDate = now.toDate().withTimeZone(fromDate.timeZone())
				def requestDateRange = new DateRange(fromDate, toDate)

				doGrabHistory(project, eventStorage, requestDateRange, config.grabChangeSizeInLines, indicator)

				historyStorage.saveGrabberConfigFor(project.name, config.withLastGrabTime(now))
			} finally {
				grabHistoryIsInProgress = false
			}
		}
	}

	private doGrabHistory(Project project, EventStorage2 eventStorage, DateRange requestDateRange,
	                      boolean grabChangeSizeInLines, indicator) {
		def dateRanges = requestDateRange.subtract(eventStorage.storedDateRange())
		def cancelled = new Cancelled() {
			@Override boolean isTrue() {
				indicator?.canceled
			}
		}
		log?.loadingProjectHistory(dateRanges.first().from, dateRanges.last().to)

		def hadErrors = false
		try {
			def minedCommits = vcsAccess.readMinedCommits(dateRanges, project, grabChangeSizeInLines, indicator, cancelled)
			for (MinedCommit minedCommit in minedCommits) {
				eventStorage.addEvents(minedCommit.fileChangeEvents)
			}
		} finally {
			eventStorage.flush()
		}

		def messageText = ""
		def dateFormatter = dd_MM_yyyy
		if (eventStorage.hasNoEvents()) {
			def from = dateFormatter.format(requestDateRange.from)
			def to = dateFormatter.format(requestDateRange.to)
			messageText += "Grabbed history to ${eventStorage.filePath}\n"
			messageText += "However, it has nothing in it probably because there are no commits from ${from} to ${to}\n"
		} else {
			def from = dateFormatter.format(eventStorage.storedDateRange().from)
			def to = dateFormatter.format(eventStorage.storedDateRange().to)
			messageText += "Grabbed history to ${eventStorage.filePath}\n"
			messageText += "It should have history from '${from}' to '${to}'.\n"
		}
		if (hadErrors) {
			messageText += "\nThere were errors while reading commits from VCS, please check IDE log for details.\n"
		}
		[text: messageText, title: "Code History Mining"]
	}

	def showCurrentFileHistoryStats(Project project) {
		def virtualFile = PluginUtil.currentFileIn(project)
		if (virtualFile == null) return

		def filePath = new LocalFilePath(virtualFile.canonicalPath, false)
		def vcsManager = project.getComponent(ProjectLevelVcsManager)

		ui.runInBackground("Looking up history for ${virtualFile.name}") { ProgressIndicator indicator ->
			def commits = []
			def allVcs = vcsManager.allVcsRoots*.vcs.unique()

			// could use this vcs.committedChangesProvider.getOneList(virtualFile, revisionNumber)
			// to get actual commits and find files in the same commit, but it's too slow and freezes UI for some reason
			for (vcs in allVcs) {
				if (!vcs?.vcsHistoryProvider?.canShowHistoryFor(virtualFile)) continue
				def session = vcs?.vcsHistoryProvider?.createSessionFor(filePath)
				if (session == null) continue
				commits.addAll(session.revisionList)
				if (indicator.canceled) return
			}
			indicator.fraction += 0.5

			if (!commits.empty) {
				def summary = createSummaryStatsFor(commits, virtualFile)
				ui.showFileHistoryStatsToolWindow(project, summary)
			} else {
				ui.showFileHasNoVcsHistory(virtualFile)
			}
		}
	}

	private static Map createSummaryStatsFor(Collection<VcsFileRevision> commits, VirtualFile virtualFile) {
		def creationDate = new Date(commits.min{it.revisionDate}.revisionDate)
		def fileAgeInDays = use(TimeCategory) {
			(Date.today().javaDate() - commits.min{it.revisionDate}.revisionDate).days
		}

		def commitsAmountByAuthor = commits
				.groupBy{ it.author.trim() }
				.collectEntries{[it.key, it.value.size()]}
				.sort{-it.value}

		def commitsAmountByPrefix = commits.groupBy{ prefixOf(it.commitMessage) }.collectEntries{[it.key, it.value.size()]}.sort{-it.value}

		[
				virtualFile: virtualFile,
				amountOfCommits: commits.size(),
				creationDate: creationDate,
				fileAgeInDays: fileAgeInDays,
				commitsAmountByAuthor: commitsAmountByAuthor.take(10),
				commitsAmountByPrefix: commitsAmountByPrefix.take(10)
		]
	}

	private static prefixOf(String commitMessage) {
		def words = commitMessage.split(" ")
		words.size() > 0 ? words[0].trim() : ""
	}

	def openQueryEditorFor(Project project, File historyFile) {
		def id = FileUtil.getNameWithoutExtension(historyFile.name) + ".groovy"
		def scriptFile = scriptsStorage.findOrCreateScriptFile(id)
		ui.openFileInIdeEditor(scriptFile, project)
	}

	def runCurrentFileAsHistoryQueryScript(Project project) {
		saveAllIdeFiles()
		def virtualFile = PluginUtil.currentFileIn(project)
		if (virtualFile == null) return
		def scriptFileName = virtualFile.name
		def scriptFolderPath = virtualFile.parent.canonicalPath

		ui.runInBackground("Running query script: $scriptFileName") { ProgressIndicator indicator ->
			def listener = new GroovyScriptRunner.Listener() {
				@Override void loadingError(String message) { ui.showQueryScriptError(scriptFileName, message, project) }
				@Override void loadingError(Throwable e) { ui.showQueryScriptError(scriptFileName, unscrambleThrowable(e), project) }
				@Override void runningError(Throwable e) { ui.showQueryScriptError(scriptFileName, unscrambleThrowable(e), project) }
			}
			def scriptRunner = new GroovyScriptRunner(listener)
			scriptRunner.loadScript(scriptFileName, scriptFolderPath)

			def historyFileName = FileUtil.getNameWithoutExtension(scriptFileName) + ".csv"
			def hasHistory = historyStorage.historyExistsFor(historyFileName)
			if (!hasHistory) return ui.showNoHistoryForQueryScript(scriptFileName)

			def cancelled = new Cancelled() {
				@Override boolean isTrue() { indicator.canceled }
			}
			def events = historyStorage.readAllEvents(historyFileName, cancelled)
			def result = scriptRunner.runScript([
					events: events, cancelled: cancelled
			])

			if (result != null) ui.showResultOfQueryScript(scriptFileName, result)
		}
	}

	private static void saveAllIdeFiles() {
		ApplicationManager.application.runWriteAction(new Runnable() {
			void run() { FileDocumentManager.instance.saveAllDocuments() }
		})
	}

	private static String unscrambleThrowable(Throwable throwable) {
		StringWriter writer = new StringWriter()
		throwable.printStackTrace(new PrintWriter(writer))
		Unscramble.normalizeText(writer.buffer.toString())
	}
}

interface CodeHistoryMinerPluginLog {
	def loadingProjectHistory(Date fromDate, Date toDate)

	def cancelledBuilding(String visualizationName)

	def measuredDuration(def entry)
}
