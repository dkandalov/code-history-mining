package history

import intellijeval.PluginUtil

import static history.SourceOfChangeLists.requestChangeListsFor
import static history.util.DateTimeUtil.dateTime

class SourceOfChangeLists_Playground {
	static playOnIt() {
		def jUnitProject = SourceOfChangeEventsGitTest.findJUnitProject()
		def changeLists = requestChangeListsFor(jUnitProject, dateTime("10:00 09/05/2013"), dateTime("17:02 09/05/2013"))

		PluginUtil.show(changeLists.collect{
			"Comment: " + it.comment + "\n" +
			"Files changed: [" + it.changes.collect{it.virtualFile.name}.join(",") + "]"
		}.join("\n"))
	}
}
