package history
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import history.events.ChangeEvent
import intellijeval.PluginUtil

class SourceOfChangeEvents {
	private final SourceOfChangeLists sourceOfChangeLists
	private final def extractChangeEvents

	SourceOfChangeEvents(SourceOfChangeLists sourceOfChangeLists, Closure<Collection<ChangeEvent>> extractChangeEvents) {
		this.sourceOfChangeLists = sourceOfChangeLists
		this.extractChangeEvents = extractChangeEvents
	}

	def request(Date historyStart, Date historyEnd, indicator = null,
	            Closure callbackWrapper = { changes, aCallback -> aCallback(changes) }, Closure callback) {
		Iterator<CommittedChangeList> changeLists = sourceOfChangeLists.fetchChangeLists(historyStart, historyEnd)
		for (changeList in changeLists) {
			if (changeList == SourceOfChangeLists.NO_MORE_CHANGE_LISTS) break
			if (indicator?.canceled) break

			callbackWrapper(changeList) {
				PluginUtil.catchingAll {
					Collection<ChangeEvent> changeEvents = extractChangeEvents(changeList)
					callback(changeEvents)
				}
			}
		}
	}
}
