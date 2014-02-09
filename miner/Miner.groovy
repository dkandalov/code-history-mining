package miner

import analysis.Context
import analysis.Visualization
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.text.DateFormatUtil
import historystorage.EventStorage
import vcsaccess.ChangeEventsReader
import vcsaccess.HistoryGrabberConfig
import historystorage.HistoryStorage
import vcsaccess.VcsAccess
import ui.UI
import util.CancelledException
import util.Measure

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
				Measure.measure("Total time") {
					def eventStorage = storage.eventStorageFor(userInput.outputFilePath)
					def eventsReader = vcsAccess.changeEventsReaderFor(project, userInput.grabChangeSizeInLines)

					def message = doGrabHistory(eventsReader, eventStorage, userInput, indicator)

					ui.showGrabbingFinishedMessage(message.text, message.title, project)
				}
				Measure.forEachDuration{ ui.log_(it) }
			}
		}
	}

	private static doGrabHistory(ChangeEventsReader eventsReader, EventStorage eventStorage, HistoryGrabberConfig config, indicator = null) {
		def updateIndicatorText = { changeList, callback ->
			log_(changeList.name)
			def date = DateFormatUtil.getDateFormat.format((Date) changeList.commitDate)
			indicator?.text = "Grabbing project history (${date} - '${changeList.comment.trim()}')"

			callback()

			indicator?.text = "Grabbing project history (${date} - looking for next commit...)"
		}
		def isCancelled = { indicator?.canceled }

		def fromDate = config.from
		def toDate = config.to + 1 // "+1" add a day to make date in UI inclusive

		def allEventWereStored = true
		def appendToStorage = { commitChangeEvents -> allEventWereStored &= eventStorage.appendToEventsFile(commitChangeEvents) }
		def prependToStorage = { commitChangeEvents -> allEventWereStored &= eventStorage.prependToEventsFile(commitChangeEvents) }

		if (eventStorage.hasNoEvents()) {
			log_("Loading project history from ${fromDate} to ${toDate}")
			eventsReader.readPresentToPast(fromDate, toDate, isCancelled, updateIndicatorText, appendToStorage)

		} else {
			if (toDate > timeAfterMostRecentEventIn(eventStorage)) {
				def (historyStart, historyEnd) = [timeAfterMostRecentEventIn(eventStorage), toDate]
				log_("Loading project history from $historyStart to $historyEnd")
				// read events from past into future because they are prepended to storage
				eventsReader.readPastToPresent(historyStart, historyEnd, isCancelled, updateIndicatorText, prependToStorage)
			}

			if (fromDate < timeBeforeOldestEventIn(eventStorage)) {
				def (historyStart, historyEnd) = [fromDate, timeBeforeOldestEventIn(eventStorage)]
				log_("Loading project history from $historyStart to $historyEnd")
				eventsReader.readPresentToPast(historyStart, historyEnd, isCancelled, updateIndicatorText, appendToStorage)
			}
		}

		def messageText = ""
		if (eventStorage.hasNoEvents()) {
			messageText += "Grabbed history to ${eventStorage.filePath}\n"
			messageText += "However, it has nothing in it probably because there are no commits from $fromDate to $toDate\n"
		} else {
			messageText += "Grabbed history to ${eventStorage.filePath}\n"
			messageText += "It should have history from '${eventStorage.oldestEventTime}' to '${eventStorage.mostRecentEventTime}'.\n"
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
