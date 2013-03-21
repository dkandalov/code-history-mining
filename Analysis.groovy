import groovy.time.TimeCategory
import org.junit.Test

import java.text.SimpleDateFormat
import java.util.regex.Matcher

import static java.lang.System.getenv

class Analysis {
	static def projectName = "idea"
//	static def projectName = "delta-flora-for-intellij"



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

		def commitsAmountByDate = events
						.groupBy{ it.revision }.collect{ it.value[0] }
						.groupBy{ floorToDay(it.revisionDate) }
						.collect{ [it.key, it.value.size()] }.sort{it[0]}
		fillTemplate("changes_size_chart_template.html", asCsvStringLiteral(commitsAmountByDate, ["date", "changes size"]))

//		def totalChangeSizeByDate = events
//						.groupBy{ floorToDay(it.revisionDate) }
//						.collect{ [it.key, it.value.sum{ (it.toOffset - it.fromOffset).abs() }] }.sort{it[0]}
//		fillTemplate("changes_size_chart_template.html", asCsvStringLiteral(totalChangeSizeByDate, ["date", "changes size"]))

//		def authorContributionByDate = events
//				.groupBy{ floorToDay(it.revisionDate) }
//				.collectEntries{ it.value = it.value.groupBy{it.author}.collect{[it.key, it.value.sum(changeSize)]}; it }
//		println(authorContributionByDate.entrySet().join("\n"))
//
//		def changesSizeByAuthorByDate = events
//				.groupBy({ it.author }, { floorToDay(it.revisionDate) })
//				.collectEntries{ it.value = fillMissingDays(it.value.collectEntries{ it.value = it.value.sum(changeSize); it }, 0).sort(); it}
//		def authorsContributions = changesSizeByAuthorByDate.entrySet().collectEntries{ [it.key, it.value.entrySet().count{it.value > 0}] }
//		def flattened = changesSizeByAuthorByDate
//				.entrySet().toList().sort{ -authorsContributions[it.key] }.take(5).collectMany { entry ->
//					entry.value.collect { [it.key, entry.key, it.value] }
//				}
//		fillTemplate("stacked_bars_template.html", asCsvStringLiteral(flattened, ["date", "author", "changes size"]))

//		def commitSizes_InOffsets = events.groupBy{ it.revision }.entrySet().collect{ [it.value.sum{changeSize(it)}] }
//		fillTemplate("commit_size_histogram_template.html", asCsvStringLiteral(commitSizes_InOffsets , ["commit size"]))
//		def commitSizes_InLines = events.groupBy{ it.revision }.entrySet().collect{ [it.value.sum{changeSizeInLines(it)}] }
//		fillTemplate("commit_size_histogram_template.html", asCsvStringLiteral(commitSizes_InLines , ["commit size"]))
//		def commitSizes_InFiles = events.groupBy{ it.revision }.entrySet().collect{ [it.value.collect{it.fileName}.unique().size()] }
//		fillTemplate("commit_size_histogram_template.html", asCsvStringLiteral(commitSizes_InFiles , ["commit size"]))

//		createCalendarViewOn(events)

//		def fileNamesInRevision = events.groupBy{ it.revision }.values()*.collect{ it.fileName }*.toList()*.unique()
//		def pairCoOccurrences = fileNamesInRevision.inject([:].withDefault{0}) { acc, files -> pairs(files).each{ acc[it.sort()] += 1 }; acc }
//															.findAll{ it.value > 3 }.sort{-it.value}
//		println(pairCoOccurrences.entrySet().join("\n"))
//
//		def nodes = pairCoOccurrences.keySet().flatten().unique().toList()
//		def relations = pairCoOccurrences.entrySet().collect{ [nodes.indexOf(it.key[0]), nodes.indexOf(it.key[1]), it.value] }
//		def nodesJSLiteral = nodes.collect{'{"name": "' + it + '", "group": 1}'}.join(",\n")
//		def relationsJSLiteral = relations.collect{'{"source": ' + it[0] + ', "target": ' + it[1] + ', "value": ' + it[2] + "}"}.join(",\n")
//		fillTemplate("cooccurrences-graph-template.html", '"nodes": [' + nodesJSLiteral + '],\n' + '"links": [' + relationsJSLiteral + ']')

		// TODO word cloud for commit messages
//		def commitMessages = events.groupBy{ it.revision }.entrySet().collect{ it.value.first().commitMessage }.toList()
//		println(commitMessages.join("\n"))

	}

	static void createCalendarViewOn(List events) {
		def changeSizeInChars = {
			def totalChangeSizeByDate = events
					.groupBy{ floorToDay(it.revisionDate) }
					.collectEntries{ [it.key, it.value.sum{ changeSize(it) }] }.sort{ it.key }
			def changesSizeRelativeToAll_ByDate = totalChangeSizeByDate.collect{ [it.key, it.value] }
			asCsvStringLiteral(changesSizeRelativeToAll_ByDate, ["date", "actualValue"])
		}()

		def changeSizeInLines = {
			def totalChangeSizeByDate = events
					.groupBy{ floorToDay(it.revisionDate) }
					.collectEntries{ [it.key, it.value.sum{ changeSizeInLines(it) }] }.sort{ it.key }
			def changesSizeRelativeToAll_ByDate = totalChangeSizeByDate.collect{ [it.key, it.value] }
			asCsvStringLiteral(changesSizeRelativeToAll_ByDate, ["date", "actualValue"])
		}()

		def changeSizeInCommits = {
			def commitsAmountByDate = events
					.groupBy{ it.revision }.collect{ it.value.first() }
					.groupBy{ floorToDay(it.revisionDate) }
					.collectEntries{ [it.key, it.value.size()] }.sort()
			def changesSizeRelativeToAll_ByDate = commitsAmountByDate.collect{ [it.key, it.value] }
			asCsvStringLiteral(changesSizeRelativeToAll_ByDate, ["date", "actualValue"])
		}()

		fillTemplate("calendar_view_template.html", "[$changeSizeInCommits,$changeSizeInLines,$changeSizeInChars]")
	}

	static def changeSize(event) { event.toOffset - event.fromOffset }
	static def changeSizeInLines(event) { event.toLine - event.fromLine }

	static void fillTemplate(String template, String jsValue) {
		def templateText = new File("html/${template}").readLines().join("\n")
		def text = templateText.replaceFirst(/(?s)\/\*data_placeholder\*\/.*\/\*data_placeholder\*\//, Matcher.quoteReplacement(jsValue))
		new File("html/${projectName}_${template.replace("_template", "")}").write(text)
	}

	static String asCsvStringLiteral(Collection values, List header) {
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
			def line = null
				while ((line = reader.readLine()) != null) {
					try {
						def (method, revision, author, time, fileName, changeType, fromLine, toLine, fromOffset, toOffset) = line.split(",")
						time = new SimpleDateFormat(EventStorage.CSV_DATE_FORMAT).parse(time)
						def commitMessage = line.substring(line.indexOf('"') + 1, line.size() - 1)

						events << new ChangeEvent(
								new PartialChangeEvent(method, fileName, changeType, fromLine.toInteger(), toLine.toInteger(), fromOffset.toInteger(), toOffset.toInteger()),
								new CommitInfo(revision, author, time, commitMessage)
						)
					} catch (Exception ignored) {
						println("Failed to parse line '${line}'")
					}
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

	static <T> Collection<Collection<T>> pairs(Collection<T> collection) {
		Collection<Collection<T>> result = collection.inject([]) { acc, value ->
			if (!acc.empty) acc.last() << value
			acc + [[value]]
		}
		if (!result.empty) result.remove(result.size() - 1)
		result
	}

	@Test void pairs_shouldGroupCollectionElementsIntoPairs() {
		assert pairs([]) == []
		assert pairs([1]) == []
		assert pairs([1, 2]) == [[1, 2]]
		assert pairs([1, 2, 3, 4, 5]) == [[1, 2], [2, 3], [3, 4], [4, 5]]
		assert pairs([a: 1, b: 2, c: 3, d: 4].entrySet()) == [[a: 1, b: 2], [b: 2, c: 3], [c: 3, d: 4]]*.entrySet()*.toList()
	}

	@Test void rgbGradient() {
		def from = [255, 255, 255]
		def to = [70, 130, 180]
		def points = 10

		def rgbStep = (0..2).collect{ (to[it] - from[it]) / (points - 1) }
		def addRgb = { rgb1, rgb2 -> (0..2).collect{ i -> rgb1[i] + rgb2[i] }}

		def interpolatedRgb =
			(0..<points-1).inject([from]) { acc, rgb -> acc + [addRgb(acc.last(), rgbStep)] }
				.collectNested{Math.round(it)}*.join(",").join("\n")

		assert interpolatedRgb == """
						|255,255,255
						|234,241,247
						|214,227,238
						|193,213,230
						|173,199,222
						|152,186,213
						|132,172,205
						|111,158,197
						|91,144,188
						|70,130,180
					""".stripMargin().trim()
	}
}

