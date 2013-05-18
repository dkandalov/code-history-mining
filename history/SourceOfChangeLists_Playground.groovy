package history
import intellijeval.PluginUtil

import static history.SourceOfChangeListsGitTest.findJUnitProject
import static history.util.DateTimeUtil.dateTime

class SourceOfChangeLists_Playground {
	static playOnIt() {
		def jUnitProject = findJUnitProject()
		def sourceOfChangeLists = new SourceOfChangeLists(jUnitProject, 1)
		def eventsExtractor = new ChangeEventsExtractor(jUnitProject)
		def eventsSource = new SourceOfChangeEvents(sourceOfChangeLists, eventsExtractor.&fileChangeEventsFrom)

		eventsSource.request(dateTime("10:00 09/05/2013"), dateTime("17:02 09/05/2013")) { changeEvents ->
			PluginUtil.show(changeEvents.join("\n"))
		}
	}
}
