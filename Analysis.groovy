import java.text.SimpleDateFormat

import static java.lang.System.getenv

class Analysis {
	static void main(String[] args) {
		lookAt("${getenv("HOME")}/Library/Application Support/IntelliJIdea12/delta-flora/intellij_eval-events.csv")
	}

	static lookAt(eventsFileName) {
		def events = []
		new File(eventsFileName).withReader { reader ->
			def line
			while ((line = reader.readLine()) != null) {
				def (method, revision, author, time, fileName, changeType, fromLine, toLine, fromOffset, toOffset) = line.split(",")
				time = new SimpleDateFormat(EventStorage.CSV_DATE_FORMAT).parse(time)
				def commitMessage = line.substring(line.indexOf('"') + 1, line.size() - 1)

				events << new ChangeEvent(
						new PartialChangeEvent(method, fileName, changeType, fromLine.toInteger(), toLine.toInteger(), fromOffset.toInteger(), toOffset.toInteger()),
						new CommitInfo(revision, author, time, commitMessage)
				)
			}
		}

		println(events.size())
	}
}

