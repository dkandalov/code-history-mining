package codehistoryminer.plugin
import codehistoryminer.core.analysis.Context
import codehistoryminer.core.analysis.FileEventsAnalytics
import codehistoryminer.core.analysis.Named
import codehistoryminer.core.analysis.values.Table
import codehistoryminer.core.analysis.values.TableList
import codehistoryminer.core.common.events.FileChangeEvent
import codehistoryminer.core.common.langutil.*
import codehistoryminer.core.historystorage.EventStorage2
import codehistoryminer.core.vcs.miner.MinedCommit
import codehistoryminer.core.visualizations.Visualization2
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

	def runAnalytics(File file, Project project, FileEventsAnalytics analytics, String analyticsName) {
		ui.runInBackground("Running ${analyticsName}") { ProgressIndicator indicator ->
			try {
				def projectName = historyStorage.guessProjectNameFrom(file.name)
				def cancelled = new Cancelled() {
					@Override boolean isTrue() { indicator.canceled }
				}

				def events = historyStorage.readAllEvents(file.name, cancelled)
				if (events.empty) {
					return ui.showNoEventsInStorageMessage(file.name, project)
				}

				def context = new Context(cancelled).withListener(new Context.Listener() {
					@Override void onLog(String message) { Logger.getInstance("CodeHistoryMining").info(message) }
				})
				context.progress.setListener(new Progress.Listener() {
					@Override void onUpdate(Progress progress) { indicator.fraction = progress.percentComplete() }
				})
				def result = analytics.analyze(events, context)
				showResultOfAnalytics(result, projectName, project)

			} catch (Cancelled ignored) {
				log?.cancelledBuilding(analyticsName)
			} catch (Exception e) {
				ui.showAnalyticsError(analyticsName, e, project)
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
			def wasLoaded = scriptRunner.loadScript(scriptFileName, scriptFolderPath)
			if (!wasLoaded) return

			def historyFileName = FileUtil.getNameWithoutExtension(scriptFileName) + ".csv"
			def hasHistory = historyStorage.historyExistsFor(historyFileName)
			if (!hasHistory) return ui.showNoHistoryForQueryScript(scriptFileName)

			def analyticsClasses = scriptRunner.loadedClasses().findAll { FileEventsAnalytics.isAssignableFrom(it) }
			if (!analyticsClasses.empty) {
				analyticsClasses.each { aClass ->
					try {
						def analytics = aClass.newInstance() as FileEventsAnalytics
						def analyticsName = analytics instanceof Named ? analytics.name() : analytics.class.simpleName
						PluginUtil.invokeOnEDT {
							runAnalytics(new File(historyFileName), project, analytics, analyticsName)
						}
					} catch (Exception e) {
						ui.showQueryScriptError(scriptFileName, unscrambleThrowable(e), project)
					}
				}

			} else {
				def analytics = new FileEventsAnalytics() {
					@Override Object analyze(List<FileChangeEvent> events, Context context) {
						scriptRunner.runScript([
								events : events,
								context: context
						])
					}
				}
				PluginUtil.invokeOnEDT {
					runAnalytics(new File(historyFileName), project, analytics, "$scriptFileName query")
				}
			}
		}
	}

	private showResultOfAnalytics(result, String projectName, Project project) {
		if (result == null) return

		if (result instanceof Visualization2) {
			def html = result.template
					.pasteInto(pluginTemplate)
					.fillProjectName(projectName)
					.inlineImports()
					.text
			ui.showInBrowser(html, projectName, "")

		} else if (result instanceof Table) {
			def file = FileUtil.createTempFile(projectName + "-result", "")
			file.renameTo(file.absolutePath + ".csv")
			file.write(result.toCsv())

			ui.openFileInIdeEditor(file, project)

		} else if (result instanceof TableList) {
			result.tables.each { table ->
				showResultOfAnalytics(table, projectName, project)
			}

		} else {
			PluginUtil.show(result)
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
