package historyreader

import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import events.FileChangeEvent
import intellijeval.PluginUtil

class ChangeEventsReader {
	private static final Closure DEFAULT_WRAPPER = { changes, aCallback -> aCallback(changes) }

	private final CommitReader commitReader
	private final def extractChangeEvents

	ChangeEventsReader(CommitReader commitReader, Closure<Collection<FileChangeEvent>> extractChangeEvents) {
		this.commitReader = commitReader
		this.extractChangeEvents = extractChangeEvents
	}

	def readPresentToPast(Date historyStart, Date historyEnd, indicator = null,
	                      Closure callbackWrapper = DEFAULT_WRAPPER, Closure callback) {
		request(historyStart, historyEnd, indicator, true, callbackWrapper, callback)
	}

	def readPastToPresent(Date historyStart, Date historyEnd, indicator = null,
	                      Closure callbackWrapper = DEFAULT_WRAPPER, Closure callback) {
		request(historyStart, historyEnd, indicator, false, callbackWrapper, callback)
	}

	private request(Date historyStart, Date historyEnd, indicator = null, boolean readPresentToPast,
	            Closure callbackWrapper, Closure callback) {
		Iterator<Commit> commits = commitReader.readCommits(historyStart, historyEnd, readPresentToPast)
		for (commit in commits) {
			if (commit == CommitReader.NO_MORE_CHANGE_LISTS) break
			if (indicator?.canceled) break

			callbackWrapper(commit) {
				PluginUtil.catchingAll {
					callback(extractChangeEvents(commit))
				}
			}
		}
	}
}
