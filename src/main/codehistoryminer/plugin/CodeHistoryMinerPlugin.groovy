package codehistoryminer.plugin
import codehistoryminer.core.analysis.Context
import codehistoryminer.core.analysis.ContextLogger
import codehistoryminer.core.analysis.EventsAnalyzer
import codehistoryminer.core.analysis.Named
import codehistoryminer.core.analysis.implementation.GroovyScriptRunner
import codehistoryminer.core.analysis.values.Table
import codehistoryminer.core.analysis.values.TableList
import codehistoryminer.core.common.events.Event
import codehistoryminer.core.common.events.FileChangeEvent
import codehistoryminer.core.common.langutil.*
import codehistoryminer.core.historystorage.TypeConverter
import codehistoryminer.core.historystorage.implementation.CSVConverter
import codehistoryminer.core.vcs.miner.MinedCommit
import codehistoryminer.core.visualizations.Visualization
import codehistoryminer.plugin.historystorage.HistoryGrabberConfig
import codehistoryminer.plugin.historystorage.HistoryStorage
import codehistoryminer.plugin.historystorage.QueryScriptsStorage
import codehistoryminer.plugin.ui.UI
import codehistoryminer.plugin.vcsaccess.VcsActions
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

import static codehistoryminer.core.common.events.FileChangeEvent.dateRangeBetween
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

	def runAnalytics(File file, Project project, EventsAnalyzer analytics, String analyticsName) {
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

				def context = new Context(cancelled).withLogger(new ContextLogger() {
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
				ui.showAnalyticsError(analyticsName, Unscramble.unscrambleThrowable(e), project)
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
                        def message = doGrabHistory(
		                        project,
		                        userInput.outputFilePath,
		                        userInput.from, userInput.to,
		                        userInput.grabChangeSizeInLines,
		                        indicator
                        )
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
				def toDate = now.toDate().withTimeZone(config.lastGrabTime.timeZone())
				doGrabHistory(project, config.outputFilePath, null, toDate, config.grabChangeSizeInLines, indicator)

				historyStorage.saveGrabberConfigFor(project.name, config.withLastGrabTime(now))
			} finally {
				grabHistoryIsInProgress = false
			}
		}
	}

	private doGrabHistory(Project project, String outputFile, Date from, Date to,
	                      boolean grabChangeSizeInLines, indicator) {
		def storageReader = historyStorage.eventStorageReader(outputFile)
		def storedDateRange = dateRangeBetween(storageReader.firstEvent(), storageReader.lastEvent())

		if (from == null) from = storedDateRange.to
		def requestDateRange = new DateRange(from, to)
		def dateRanges = requestDateRange.subtract(storedDateRange)
		def cancelled = new Cancelled() {
			@Override boolean isTrue() {
				indicator?.canceled
			}
		}
		log?.loadingProjectHistory(dateRanges.first().from, dateRanges.last().to)

		def hadErrors = false
		def storageWriter = historyStorage.eventStorageWriter(outputFile)
		try {
			def minedCommits = vcsAccess.readMinedCommits(dateRanges, project, grabChangeSizeInLines, indicator, cancelled)
			for (MinedCommit minedCommit in minedCommits) {
				storageWriter.addEvents(minedCommit.eventList)
			}
		} finally {
			storageWriter.flush()
		}

		def messageText = ""
		if (storageReader.hasNoEvents()) {
			messageText += "Grabbed history to ${outputFile}\n"
			messageText += "However, it has nothing in it probably because there are no commits ${formatRange(requestDateRange)}\n"
		} else {
			def newStoredDateRange = dateRangeBetween(storageReader.firstEvent(), storageReader.lastEvent())
			messageText += "Grabbed history to ${outputFile}\n"
			messageText += "It should have history ${formatRange(newStoredDateRange)}'.\n"
		}
		if (hadErrors) {
			messageText += "\nThere were errors while reading commits from VCS, please check IDE log for details.\n"
		}
		[text: messageText, title: "Code History Mining"]
	}

	private static String formatRange(DateRange dateRange) {
		def from = dd_MM_yyyy.format(dateRange.from)
		def to = dd_MM_yyyy.format(dateRange.to)
		"from '${from}' to '${to}'"
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
		def scriptFilePath = virtualFile.canonicalPath
		def scriptFileName = virtualFile.name

		ui.runInBackground("Running query script: $scriptFileName") { ProgressIndicator indicator ->
			def listener = new GroovyScriptRunner.Listener() {
				@Override void loadingError(String message) { ui.showQueryScriptError(scriptFileName, message, project) }
				@Override void loadingError(Throwable e) { ui.showQueryScriptError(scriptFileName, Unscramble.unscrambleThrowable(e), project) }
				@Override void runningError(Throwable e) { ui.showQueryScriptError(scriptFileName, Unscramble.unscrambleThrowable(e), project) }
			}
			def scriptRunner = new GroovyScriptRunner(listener)
			def wasLoaded = scriptRunner.loadScript(scriptFilePath)
			if (!wasLoaded) return

			def historyFileName = FileUtil.getNameWithoutExtension(scriptFileName) + ".csv"
			def hasHistory = historyStorage.historyExistsFor(historyFileName)
			if (!hasHistory) return ui.showNoHistoryForQueryScript(scriptFileName)

			def analyticsClasses = scriptRunner.loadedClasses().findAll { EventsAnalyzer.isAssignableFrom(it) }
			if (!analyticsClasses.empty) {
				analyticsClasses.each { aClass ->
					try {
						def analytics = aClass.newInstance() as EventsAnalyzer
						def analyticsName = analytics instanceof Named ? analytics.name() : analytics.class.simpleName
						PluginUtil.invokeOnEDT {
							runAnalytics(new File(historyFileName), project, analytics, analyticsName)
						}
					} catch (Exception e) {
						ui.showQueryScriptError(scriptFileName, Unscramble.unscrambleThrowable(e), project)
					}
				}

			} else {
				def analytics = new EventsAnalyzer() {
					@Override Object analyze(List<Event> eventList, Context context) {
						def events = eventList.collect{ new FileChangeEvent(it) }
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

		if (result instanceof Visualization) {
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

		} else if (result instanceof List) {
			if (result.isEmpty() || !(result.get(0) instanceof Event)) {
				result = result.collect{it.toString()}.join("\n")

				def file = FileUtil.createTempFile(projectName + "-result", "")
				file.renameTo(file.absolutePath + ".csv")
				file.write(result)
				ui.openFileInIdeEditor(file, project)
			} else {
				def events = result as List<Event>
				def timeZone = TimeZone.default
				def converter = new CSVConverter(TypeConverter.Default.create(timeZone))
				result = events.collect{ converter.toCsv(it) }.join("\n")

				def file = FileUtil.createTempFile(projectName + "-result", "")
				file.renameTo(file.absolutePath + ".csv")
				file.write(result)
				ui.openFileInIdeEditor(file, project)
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
}

interface CodeHistoryMinerPluginLog {
	def loadingProjectHistory(Date fromDate, Date toDate)

	def cancelledBuilding(String visualizationName)

	def measuredDuration(def entry)
}
