package history

import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import history.events.ChangeEvent
import intellijeval.PluginUtil

class SourceOfChangeEvents {
	private final CommitReader commitReader
	private final def extractChangeEvents

	SourceOfChangeEvents(CommitReader commitReader, Closure<Collection<ChangeEvent>> extractChangeEvents) {
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
					Collection<ChangeEvent> changeEvents = extractChangeEvents(commit)
					callback(changeEvents)
				}
			}
		}
	}
}
