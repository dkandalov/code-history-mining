package analysis
import groovy.time.TimeCategory
import history.events.FileChangeEvent
import history.events.FileChangeInfo

import java.text.SimpleDateFormat

import static analysis.Analysis.Util.*

class Analysis {
	static String createJsonForCommitsStackBarsChart(Collection<FileChangeEvent> events) {
		def fromDay = floorToDay(events.last().revisionDate)
		def toDay = floorToDay(events.first().revisionDate)
		def addMissingDays = { valuesByDate ->
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
				it.value = addMissingDays(it.value).sort()
				it
			}
		def amountOfCommitsByAuthor = changesByAuthorByDate.entrySet().collectEntries{
			def author = it.key
			def commits = it.value.values()
			[author, commits.size()]
		}
		def numberOfTopCommitters = 5
		def flattened = changesByAuthorByDate.entrySet().toList()
				.sort{ -amountOfCommitsByAuthor[it.key] }
				.take(numberOfTopCommitters)
				.collectMany { entry ->
					entry.value.collect { [it.key, entry.key, it.value.size()] }
				}
		asCsvStringLiteral(flattened, ["date", "author", "amount of commits"])
	}

	static void createJsonForAmountOfComittersBarsChart(List<FileChangeEvent> events) {
		def comittersByDay = events
				.groupBy{ floorToDay(it.revisionDate) }
				.collectEntries{ [it.key, it.value.collect{it.author}.unique()]}
				.sort{ it.key }
		println(comittersByDay.entrySet().join("\n"))
	}

	static void createJsonForAverageAmountOfLinesChangedChart(List<FileChangeEvent> events) {
		def averageChangeSize = { eventList ->
			if (eventList.empty) 0
			else eventList.sum(0){changeSizeInLines(it)} / eventList.size()
		}
		def averageChangeSizeByDay = events
				.groupBy{ floorToDay(it.revisionDate) }
				.collectEntries{ [it.key, averageChangeSize(it.value)] }
				.sort{ it.key }
		println(averageChangeSizeByDay.entrySet().join("\n"))
	}

	static void createJsonForAverageAmountOfFilesChangedChart(List<FileChangeEvent> events) {
		def averageChangeSize = { eventsByRevision ->
			if (eventsByRevision.empty) 0
			else eventsByRevision.entrySet().sum(0){ it.value.collect{it.fileName}.unique().size() } / eventsByRevision.size()
		}
		def changeSizeByDay = events
				.groupBy({floorToDay(it.revisionDate) }, {it.revision})
				.collectEntries{ [it.key, averageChangeSize(it.value)] }
				.sort{ it.key }
		println(changeSizeByDay.entrySet().join("\n"))
	}

	static class TreeMapView {
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

		private static class Container {
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
		def totalChangeSizeInCharsByDate = eventsByDay.collect{ [it.key, it.value.sum{ changeSizeInChars(it) }] }.sort{it[0]}
		def totalChangeInLinesSizeByDate = eventsByDay.collect{ [it.key, it.value.sum{ changeSizeInLines(it) }] }.sort{it[0]}

		def changeSizeInCommits = asCsvStringLiteral(commitsAmountByDate, ["date", "changeSize"])
		def changeSizeInChars = asCsvStringLiteral(totalChangeSizeInCharsByDate, ["date", "changeSize"])
		def changeSizeInLines = asCsvStringLiteral(totalChangeInLinesSizeByDate, ["date", "changeSize"])

		"[$changeSizeInCommits,$changeSizeInLines,$changeSizeInChars]"
	}

	static String createJsonForCalendarView(List<FileChangeEvent> events) {
		def changeSizeInChars = {
			def totalChangeSizeByDate = events
					.groupBy{ floorToDay(it.revisionDate) }
					.collectEntries{ [it.key, it.value.sum{ changeSizeInChars(it) }] }.sort{ it.key }
			def changesSizeRelativeToAll_ByDate = totalChangeSizeByDate.collect{ [it.key, it.value] }
			asCsvStringLiteral(changesSizeRelativeToAll_ByDate, ["date", "changeSize"])
		}()

		def changeSizeInLines = {
			def totalChangeSizeByDate = events
					.groupBy{ floorToDay(it.revisionDate) }
					.collectEntries{ [it.key, it.value.sum{ changeSizeInLines(it) }] }.sort{ it.key }
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
		static def changeSizeInChars(FileChangeEvent event) {
			if (event.chars == FileChangeInfo.NA || event.chars == FileChangeInfo.TOO_BIG_TO_DIFF) 0
			else event.chars.added + event.chars.modified + event.chars.removed
		}
		static def changeSizeInLines(FileChangeEvent event) {
			if (event.lines == FileChangeInfo.NA || event.lines == FileChangeInfo.TOO_BIG_TO_DIFF) 0
			else event.lines.added + event.lines.modified + event.lines.removed
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
	}
}

