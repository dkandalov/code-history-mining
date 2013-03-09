import groovy.time.TimeCategory

import java.text.SimpleDateFormat
import java.util.regex.Matcher

import static java.lang.System.getenv

class Analysis {
	static def projectName = "idea"

	static void main(String[] args) {
		def events = loadAllEvents("${getenv("HOME")}/Library/Application Support/IntelliJIdea12/delta-flora/${projectName}-events.csv")
		def fromDay = floorToDay(events.last().revisionDate)
		def toDay = floorToDay(events.first().revisionDate)
		def fillMissingDays = { valuesByDate, defaultValue ->
			use(TimeCategory) {
				def day = fromDay.clone()
				while (!day.after(toDay)) {
					if (!valuesByDate.containsKey(day)) valuesByDate[day] = defaultValue
					day += 1.day
				}
				valuesByDate
			}
		}
		def changeSize = { it.toOffset - it.fromOffset }

//		def commitsAmountByDate = events
//						.groupBy{ it.revision }.collect{ it.value[0] }
//						.groupBy{ floorToDay(it.revisionDate) }
//						.collect{ [it.key, it.value.size()] }.sort{it[0]}
//		fillTemplate("commit_count_template.html", asJavaScriptLiteral(commitsAmountByDate, ["date", "amount of commits"]))
//
//		def totalChangeSizeByDate = events
//						.groupBy{ floorToDay(it.revisionDate) }
//						.collect{ [it.key, it.value.sum{ (it.toOffset - it.fromOffset).abs() }] }.sort{it[0]}
//		fillTemplate("changes_size_template.html", asJavaScriptLiteral(totalChangeSizeByDate, ["date", "changes size"]))

//		def authorContributionByDate = events
//			.groupBy{ floorToDay(it.revisionDate) }
//			.collectEntries{ it.value = it.value.groupBy{it.author}.collect{[it.key, it.value.sum(changeSize)]}; it }
//		println(authorContributionByDate.entrySet().join("\n"))

		def changesSizeByAuthorByDate = events
				.groupBy({ it.author }, { floorToDay(it.revisionDate) })
				.collectEntries{ it.value = fillMissingDays(it.value.collectEntries{ it.value = it.value.sum(changeSize); it }, 0).sort(); it}
		def flattened = changesSizeByAuthorByDate
				.entrySet().toList().collectMany { entry ->
					entry.value.collect { [it.key, entry.key, it.value] }
				}
		fillTemplate("stacked_bars_template.html", asJavaScriptLiteral(flattened, ["date", "author", "changes size"]))
	}

	static void fillTemplate(String template, String jsValue) {
		println(jsValue)
		def templateText = new File("html/${template}").readLines().join("\n")
		def text = templateText.replaceFirst(/(?s)\/\*data_placeholder\*\/.*\/\*data_placeholder\*\//, Matcher.quoteReplacement(jsValue))
		new File("html/${projectName}_${template.replace("_template", "")}").write(text)
	}

	static String asJavaScriptLiteral(Collection values, List header) {
		def formatDate = { Date date -> new SimpleDateFormat("dd/MM/yyyy").format(date) }

		def jsNewLine = "\\n\\\n"
		def jsHeader = header.join(",") + jsNewLine
		def jsBody = values
				.collect{ it.collect{it instanceof Date ? formatDate(it) : it} }
				.collect{it.join(",")}
				.join(jsNewLine)
		"\"\\\n" + jsHeader + jsBody + jsNewLine + "\"";
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

	static Date floorToDay(Date date) {
		date[Calendar.MILLISECOND] = 0
		date[Calendar.SECOND] = 0
		date[Calendar.MINUTE] = 0
		date[Calendar.HOUR_OF_DAY] = 0
		date
	}
}

