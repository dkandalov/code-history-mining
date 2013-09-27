package historyreader

import com.intellij.openapi.diagnostic.Logger
import events.EventStorage
import ui.DialogState

import static com.intellij.util.text.DateFormatUtil.getDateFormat

class HistoryGrabber {
	static doGrabHistory(ChangeEventsReader eventsReader, EventStorage storage, DialogState userInput, indicator = null) {
		def updateIndicatorText = { changeList, callback ->
			log_(changeList.name)
			def date = dateFormat.format((Date) changeList.commitDate)
			indicator?.text = "Grabbing project history (${date} - '${changeList.comment.trim()}')"

			callback()

			indicator?.text = "Grabbing project history (${date} - looking for next commit...)"
		}
		def isCancelled = { indicator?.canceled }

		def fromDate = userInput.from
		def toDate = userInput.to + 1 // "+1" add a day to make date in UI inclusive

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
			messageText += "\nSome of events were not added to csv file because it already contains events within this time range\n"
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
