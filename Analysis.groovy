import history.EventStorage
import org.junit.Test

import java.text.SimpleDateFormat
import java.util.regex.Matcher

import static java.lang.System.getenv

class Analysis {
	static def projectName = "scratch"

	static void main(String[] args) {
		def filePath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea12/delta-flora/${projectName}-events.csv"
		def events = new EventStorage(filePath).readAllEvents { println("Failed to parse line '${it}'") }

//		fillTemplate("calendar_view.html", createJsonForCalendarView(events))
//		fillTemplate("changes_size_chart.html", createJsonForBarChartView(events))
//		fillTemplate("cooccurrences-graph.html", createJsonForCooccurrencesGraph(events))
//		createChangeSizeTreeMapFor(events)

		// TODO word cloud for commit messages
//		def commitMessages = events.groupBy{ it.revision }.entrySet().collect{ it.value.first().commitMessage }.toList()
//		println(commitMessages.join("\n"))
	}

	static void createChangeSizeTreeMapFor(events) {
		// TODO
	}

	static String createJsonForCooccurrencesGraph(events, threshold = 7) {
		def fileNamesInRevision = events.groupBy{ it.revision }.values()*.collect{ it.fileName }*.toList()*.unique()
		def pairCoOccurrences = fileNamesInRevision.inject([:].withDefault{0}) { acc, files -> pairs(files).each{ acc[it.sort()] += 1 }; acc }
															.findAll{ it.value > threshold }.sort{-it.value}
		println(pairCoOccurrences.entrySet().join("\n"))

		def nodes = pairCoOccurrences.keySet().flatten().unique().toList()
		def relations = pairCoOccurrences.entrySet().collect{ [nodes.indexOf(it.key[0]), nodes.indexOf(it.key[1]), it.value] }
		def nodesJSLiteral = nodes.collect{'{"name": "' + it + '", "group": 1}'}.join(",\n")
		def relationsJSLiteral = relations.collect{'{"source": ' + it[0] + ', "target": ' + it[1] + ', "value": ' + it[2] + "}"}.join(",\n")

		'"nodes": [' + nodesJSLiteral + '],\n' + '"links": [' + relationsJSLiteral + ']'
	}

	static String createJsonForBarChartView(List events) {
		def eventsByDay = events.groupBy{ floorToDay(it.revisionDate) }

		def commitsAmountByDate = eventsByDay
						.collect{ [it.key, it.value.groupBy{ it.revision }.size()] }.sort{it[0]}
		def changeSizeInCommits = asCsvStringLiteral(commitsAmountByDate, ["date", "changeSize"])

		def totalChangeSizeInCharsByDate = eventsByDay
						.collect{ [it.key, it.value.sum{ changeSizeOf(it) }] }.sort{it[0]}
		def changeSizeInChars = asCsvStringLiteral(totalChangeSizeInCharsByDate, ["date", "changeSize"])

		def totalChangeInLinesSizeByDate = eventsByDay
						.collect{ [it.key, it.value.sum{ changeSizeInLinesOf(it) }] }.sort{it[0]}
		def changeSizeInLines = asCsvStringLiteral(totalChangeInLinesSizeByDate, ["date", "changeSize"])

		"[$changeSizeInCommits,$changeSizeInLines,$changeSizeInChars]"
	}

	static String createJsonForCalendarView(List events) {
		def changeSizeInChars = {
			def totalChangeSizeByDate = events
					.groupBy{ floorToDay(it.revisionDate) }
					.collectEntries{ [it.key, it.value.sum{ changeSizeOf(it) }] }.sort{ it.key }
			def changesSizeRelativeToAll_ByDate = totalChangeSizeByDate.collect{ [it.key, it.value] }
			asCsvStringLiteral(changesSizeRelativeToAll_ByDate, ["date", "changeSize"])
		}()

		def changeSizeInLines = {
			def totalChangeSizeByDate = events
					.groupBy{ floorToDay(it.revisionDate) }
					.collectEntries{ [it.key, it.value.sum{ changeSizeInLinesOf(it) }] }.sort{ it.key }
			def changesSizeRelativeToAll_ByDate = totalChangeSizeByDate.collect{ [it.key, it.value] }
			asCsvStringLiteral(changesSizeRelativeToAll_ByDate, ["date", "changeSize"])
		}()

		def changeSizeInCommits = {
			def commitsAmountByDate = events
					.groupBy{ it.revision }.collect{ it.value.first() }
					.groupBy{ floorToDay(it.revisionDate) }
					.collectEntries{ [it.key, it.value.size()] }.sort()
			def changesSizeRelativeToAll_ByDate = commitsAmountByDate.collect{ [it.key, it.value] }
			asCsvStringLiteral(changesSizeRelativeToAll_ByDate, ["date", "changeSize"])
		}()

		"[$changeSizeInCommits,$changeSizeInLines,$changeSizeInChars]"
	}

	static def changeSizeOf(event) { event.toOffset - event.fromOffset }
	static def changeSizeInLinesOf(event) { event.toLine - event.fromLine }

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

