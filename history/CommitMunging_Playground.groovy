package history
import intellijeval.PluginUtil

import static history.util.DateTimeUtil.dateTime

class CommitMunging_Playground {
	static playOnIt() {
		def project = CommitReaderGitTest.findProject("junit")
		def commitReader = new CommitReader(project, 5)
		def commitFilesMunger = new CommitFilesMunger(project, true)
		def eventsReader = new ChangeEventsReader(commitReader, commitFilesMunger.&mungeCommit)

		PluginUtil.doInBackground("playground", {
			eventsReader.readPresentToPast(dateTime("19:37 01/04/2013"), dateTime("19:40 01/04/2013")) { changeEvents ->
				PluginUtil.show(changeEvents.collect{it}.join("\n"))
			}
			eventsReader.readPresentToPast(dateTime("15:42 03/10/2007"), dateTime("15:43 03/10/2007")) { changeEvents ->
				PluginUtil.show(changeEvents.collect{it}.join("\n"))
			}
//			PluginUtil.show("----------")
//			eventsReader.request(dateTime("10:00 23/02/2013"), dateTime("17:02 27/02/2013")) { changeEvents ->
//				PluginUtil.show(changeEvents.collect{it.revisionDate}.join("\n"))
//			}
		}, {})
	}
}
