package analysis
import groovy.time.TimeCategory
import history.events.FileChangeEvent
import org.junit.Test

import java.text.SimpleDateFormat

import static analysis.Analysis.Util.*

class Analysis {
	static String createJsonForCommitsStackBarsChart(events) {
		def fromDay = floorToDay(events.last().revisionDate)
		def toDay = floorToDay(events.first().revisionDate)
		def fillMissingDays = { valuesByDate ->
			use(TimeCategory) {
				def day = fromDay.clone()
				while (!day.after(toDay)) {
					if (!valuesByDate.containsKey(day)) valuesByDate[day] = []
					day += 1.day
				}
				valuesByDate
			}
		}

		def changesByAuthorByDate = events
			.groupBy({ it.author }, { floorToDay(it.revisionDate) })
			.collectEntries {
				def changesSizeByDate = it.value
				it.value = fillMissingDays(changesSizeByDate).sort()
				it
			}
		def authorsContributions = changesByAuthorByDate.entrySet().collectEntries{
			def author = it.key
			def commits = it.value.values()
			[author, commits.size()]
		}
		def numberOfTopCommitters = 5
		def flattened = changesByAuthorByDate.entrySet().toList()
				.sort{ -authorsContributions[it.key] }.take(numberOfTopCommitters)
				.collectMany { entry ->
					entry.value.collect { [it.key, entry.key, it.value.size()] }
				}
		asCsvStringLiteral(flattened, ["date", "author", "changeSizeOf"])
	}

	static class TreeMap {
		static String createJsonForChangeSizeTreeMap(events) {
			events = events.groupBy{ [it.revision, it.packageBefore, it.packageAfter] }
					.collect{ it.value.first() }
					.collectMany{
				if (!it.packageBefore.empty && !it.packageAfter.empty && it.packageBefore != it.packageAfter) {
					[it.packageBefore + "/" + it.fileName, it.packageAfter + "/" + it.fileName]
				} else {
					[(!it.packageBefore.empty ? it.packageBefore : it.packageAfter) + "/" + it.fileName]
				}
			}

			def containerTree = new Container("", 0)
			events.inject(containerTree) { Container tree, filePath -> tree.updateTree(filePath) }
			containerTree.firstChild.toJSON()
		}

		static class Container {
			private final String name
			private final Collection<Container> children
			private int commits

			Container(String name, Collection<Container> children = new ArrayList(), int commits) {
				this.name = name
				this.children = children.findAll{ it.commits > 0 }
				this.commits = commits
			}

			Container updateTree(String filePath) {
				doUpdateTree(filePath.split("/").toList(), this)
				this
			}

			private static doUpdateTree(List<String> filePath, Container container) {
				container.plusCommit()

				if (filePath.empty) return

				def matchingChild = container.children.find{ it.name == filePath.first() }
				if (matchingChild == null)
					matchingChild = container.addChild(new Container(filePath.first(), 0))

				doUpdateTree(filePath.tail(), matchingChild)
			}

			Container getFirstChild() { children.first() }

			private Container addChild(Container container) {
				children.add(container)
				container
			}

			private def plusCommit() {
				commits++
			}

			String toJSON() {
				String childrenAsJSON = "\"children\": [\n" + children.collect { it.toJSON() }.join(',\n') + "]"
				"{" +
						"\"name\": \"$name\", " +
						"\"size\": \"$commits\", " +
						childrenAsJSON +
						"}"
			}
		}
	}

	static String createJsonForCommitCommentWordCloud(events) {
		Map wordOccurrences = events.groupBy{ it.revision }.entrySet()
				.collect{ it.value.first().commitMessage }.toList()
				.collectMany{ it.split(/[\s!{}\[\]+-<>()\/\\,"'@&$=*\|\?]/).findAll{ !it.empty }.collect{it.toLowerCase()} }
				.inject([:].withDefault{0}) { wordFrequency, word ->
			wordFrequency.put(word, wordFrequency[word] + 1)
			wordFrequency
		}
		def mostFrequentWords = wordOccurrences.entrySet().sort{ -it.value }.take(600)

		"""{"words": [
${mostFrequentWords.collect { '{"text": "' + it.key + '", "size": ' + it.value + '}' }.join(",\n")}
]}
"""
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

	static String createJsonForBarChartView(List<FileChangeEvent> events) {
		def eventsByDay = events.groupBy{ floorToDay(it.revisionDate) }

		def commitsAmountByDate = eventsByDay.collect{ [it.key, it.value.groupBy{ it.revision }.size()] }.sort{it[0]}
		def totalChangeSizeInCharsByDate = eventsByDay.collect{ [it.key, it.value.sum{ changeSizeInCharsOf(it) }] }.sort{it[0]}
		def totalChangeInLinesSizeByDate = eventsByDay.collect{ [it.key, it.value.sum{ changeSizeInLinesOf(it) }] }.sort{it[0]}

		def changeSizeInCommits = asCsvStringLiteral(commitsAmountByDate, ["date", "changeSize"])
		def changeSizeInChars = asCsvStringLiteral(totalChangeSizeInCharsByDate, ["date", "changeSize"])
		def changeSizeInLines = asCsvStringLiteral(totalChangeInLinesSizeByDate, ["date", "changeSize"])

		"[$changeSizeInCommits,$changeSizeInLines,$changeSizeInChars]"
	}

	static String createJsonForCalendarView(List events) {
		def changeSizeInChars = {
			def totalChangeSizeByDate = events
					.groupBy{ floorToDay(it.revisionDate) }
					.collectEntries{ [it.key, it.value.sum{ changeSizeInCharsOf(it) }] }.sort{ it.key }
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

	static class Util {
		static def changeSizeInCharsOf(FileChangeEvent event) { event.chars.added + event.chars.modified + event.chars.removed }
		static def changeSizeInLinesOf(FileChangeEvent event) { event.lines.added + event.lines.modified + event.lines.removed }

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

