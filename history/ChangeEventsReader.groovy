package history

import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import history.events.FileChangeEvent
import intellijeval.PluginUtil

class ChangeEventsReader {
	private final CommitReader commitReader
	private final def extractChangeEvents

	ChangeEventsReader(CommitReader commitReader, Closure<Collection<FileChangeEvent>> extractChangeEvents) {
		this.commitReader = commitReader
		this.extractChangeEvents = extractChangeEvents
	}

	def request(Date historyStart, Date historyEnd, indicator = null,
	            Closure callbackWrapper = { changes, aCallback -> aCallback(changes) }, Closure callback) {
		Iterator<Commit> commits = commitReader.readCommits(historyStart, historyEnd)
		for (commit in commits) {
			if (commit == CommitReader.NO_MORE_CHANGE_LISTS) break
			if (indicator?.canceled) break

			callbackWrapper(commit) {
				PluginUtil.catchingAll {
					Collection<FileChangeEvent> changeEvents = extractChangeEvents(commit)
					callback(changeEvents)
				}
			}
		}
	}
}
