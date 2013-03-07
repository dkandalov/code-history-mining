import java.text.SimpleDateFormat

import static java.lang.System.getenv

class Analysis {
	static void main(String[] args) {
		def events = loadAllEvents("${getenv("HOME")}/Library/Application Support/IntelliJIdea12/delta-flora/intellij_eval-events.csv")
		def aa = events.groupBy { it.revision }
		println(aa.entrySet().collect{[it.key, it.value[0].commitMessage]}.join("\n"))
		def amountByDate = aa.collect{ /*println(it.value[0].commitMessage);*/ it.value[0] }
				.groupBy{ resetToDays(it.revisionDate) }.collect{ [it.key, it.value.size()] }.sort{it[0]}
		println(amountByDate.join("\n"))
	}

	static loadAllEvents(String eventsFileName) {
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
		events
	}

	static Date resetToDays(Date date) {
		date[Calendar.MILLISECOND] = 0
		date[Calendar.SECOND] = 0
		date[Calendar.MINUTE] = 0
		date[Calendar.HOUR_OF_DAY] = 0
		date
	}
}

