package history
import intellijeval.PluginUtil

import static history.util.DateTimeUtil.dateTime

class CommitMunging_Playground {
	static playOnIt() {
		def project = CommitReaderGitTest.findProject("delta-flora-for-intellij")
		def commitReader = new CommitReader(project, 5)
		def commitFilesMunger = new CommitFilesMunger(project, true)
		def eventsReader = new ChangeEventsReader(commitReader, commitFilesMunger.&mungeCommit)

		PluginUtil.doInBackground("playground", {
			eventsReader.readPresentToPast(dateTime("10:00 22/05/2013"), dateTime("17:02 24/05/2013")) { changeEvents ->
				PluginUtil.show(changeEvents.collect{it.revisionDate}.join("\n"))
			}
//			PluginUtil.show("----------")
//			eventsReader.request(dateTime("10:00 23/02/2013"), dateTime("17:02 27/02/2013")) { changeEvents ->
//				PluginUtil.show(changeEvents.collect{it.revisionDate}.join("\n"))
//			}
		}, {})
	}
}
