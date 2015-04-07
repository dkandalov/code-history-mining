package codemining.plugin
import codemining.core.common.langutil.*
import codemining.core.historystorage.EventStorage
import codemining.core.vcs.MinedCommit
import codemining.core.vcs.MiningCommitReader
import codemining.core.visualizations.Visualization
import codemining.historystorage.HistoryGrabberConfig
import codemining.historystorage.HistoryStorage
import codemining.plugin.ui.UI
import codemining.vcsaccess.VcsActions
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import groovy.time.TimeCategory
import liveplugin.PluginUtil

import static codemining.core.common.langutil.Date.Formatter.dd_MM_yyyy

class CodeMiningPlugin {
	private final UI ui
	private final HistoryStorage storage
	private final VcsActions vcsAccess
	private final Measure measure
	private final CodeMiningPluginLog log
	private volatile boolean grabHistoryIsInProgress

	CodeMiningPlugin(UI ui, HistoryStorage storage, VcsActions vcsAccess, Measure measure, CodeMiningPluginLog log = null) {
		this.ui = ui
		this.storage = storage
		this.vcsAccess = vcsAccess
		this.measure = measure
		this.log = log
	}

	def createVisualization(File file, Visualization visualization) {
		ui.runInBackground("Creating ${visualization.name.toLowerCase()}") { ProgressIndicator indicator ->
			try {
				measure.start()

				def projectName = storage.guessProjectNameFrom(file.name)
				def cancelled = new Cancelled() {
					@Override boolean isTrue() { indicator.canceled }
				}

				def events = storage.readAllEvents(file.name, cancelled)
				def listener = new Visualization.Listener() {
					@Override void onProgress(Progress progress) { indicator.fraction = progress.percentComplete() }
					@Override void onLog(String message) { Logger.getInstance("CodeHistoryMining").info(message) }
				}
				def html = visualization.generateFrom(events, projectName, cancelled, listener)

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
		FileTypeManager.instance.registeredFileTypes.inject([:]) { Map map, FileType fileType ->
			int fileCount = FileBasedIndex.instance.getContainingFiles(FileTypeIndex.NAME, fileType, scope).size()
			if (fileCount > 0) map.put(fileType.defaultExtension, fileCount)
			map
		}.sort{ -it.value }
	}

	def onProjectOpened(Project project) {
		def grabberConfig = storage.loadGrabberConfigFor(project.name)
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
			storage.saveGrabberConfigFor(project.name, userInput)
		}

		def grabberConfig = storage.loadGrabberConfigFor(project.name)
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
						def eventStorage = storage.eventStorageFor(userInput.outputFilePath)
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
		def config = storage.loadGrabberConfigFor(project.name)
		now = now.withTimeZone(config.lastGrabTime.timeZone())
		if (config.lastGrabTime.floorToDay() == now.floorToDay()) return

		grabHistoryIsInProgress = true
		ui.runInBackground("Grabbing project history") { ProgressIndicator indicator ->
			try {
				def eventStorage = storage.eventStorageFor(config.outputFilePath)
				def fromDate = eventStorage.storedDateRange().to
				def toDate = now.toDate().withTimeZone(fromDate.timeZone())
				def requestDateRange = new DateRange(fromDate, toDate)

				doGrabHistory(project, eventStorage, requestDateRange, config.grabChangeSizeInLines, indicator)

				storage.saveGrabberConfigFor(project.name, config.withLastGrabTime(now))
			} finally {
				grabHistoryIsInProgress = false
			}
		}
	}

	private doGrabHistory(Project project, EventStorage eventStorage, DateRange requestDateRange,
						  boolean grabChangeSizeInLines, indicator) {
		def dateRanges = requestDateRange.subtract(eventStorage.storedDateRange())
		def progress = new Progress(dateRanges.sum{ it.durationInDays() } as long).with(new Progress.Listener() {
			@Override void onUpdate(Progress progress) {
				indicator?.fraction = progress.percentComplete()
			}
		})
		def cancelled = new Cancelled() {
			@Override boolean isTrue() {
				indicator?.canceled
			}
		}

		def hadErrors = false
		try {
			for (DateRange dateRange in dateRanges) {
				log?.loadingProjectHistory(dateRange.from, dateRange.to)

				def minedCommits = vcsAccess.readMinedCommits(dateRange, project, grabChangeSizeInLines, progress, cancelled)

				for (MinedCommit minedCommit in minedCommits) {
					if (minedCommit == MiningCommitReader.failedToMine) {
						hadErrors = true
					} else {
						eventStorage.addEvents(minedCommit.fileChangeEvents)
					}
				}
				eventStorage.flush()
			}
		} catch (Cancelled ignore) {}

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

		def filePath = new FilePathImpl(virtualFile)
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
}

interface CodeMiningPluginLog {
	def loadingProjectHistory(Date fromDate, Date toDate)

	def processingChangeList(String changeListName)

	def cancelledBuilding(String visualizationName)

	def measuredDuration(def entry)
}
