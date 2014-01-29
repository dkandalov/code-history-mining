package historyreader
import com.intellij.openapi.project.Project
import events.ChangeStats
import events.CommitInfo
import events.FileChangeEvent
import events.FileChangeInfo
import org.junit.Test

import static events.ChangeStats.NA
import static historyreader.CommitReaderGitTest.findJUnitProject
import static util.DateTimeUtil.dateTime
import static util.DateTimeUtil.exactDateTime

class ChangeEventsReaderGitTest {

	@Test "should read file events"() {
		def countChangeSizeInLines = false
		def commitMunger = new CommitFilesMunger(jUnitProject, countChangeSizeInLines)

		def changeEvents = readChangeEvents(fromDate, toDate, jUnitProject, commitMunger)

		assert asString(changeEvents) == asString([
				fileChangeEvent(commitInfo, fileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", "MODIFICATION", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("TheoryMethod.java", "TheoryMethodRunner.java", "/src/org/junit/experimental/theories/internal", "/src/org/junit/experimental/theories/internal", "MOVED", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4ClassRunner.java", "", "/src/org/junit/internal/runners", "MODIFICATION", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4MethodRunner.java", "", "/src/org/junit/internal/runners", "NEW", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethod.java", "", "/src/org/junit/internal/runners", "MODIFICATION", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheories.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheoryMethod.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethodInterfaceTest.java", "", "/src/org/junit/tests/extension", "MODIFICATION", NA, NA))
		])
	}

	@Test "should read file events with change size details"() {
		def countChangeSizeInLines = true
		def commitMunger = new CommitFilesMunger(jUnitProject, countChangeSizeInLines)

		def changeEvents = readChangeEvents(fromDate, toDate, jUnitProject, commitMunger)

		assert asString(changeEvents) == asString([
				fileChangeEvent(commitInfo, fileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", "MODIFICATION", changeStats(37, 37, 0, 4, 0), changeStats(950, 978, 0, 215, 0))),
				fileChangeEvent(commitInfo, fileChangeInfo("TheoryMethod.java", "TheoryMethodRunner.java", "/src/org/junit/experimental/theories/internal", "/src/org/junit/experimental/theories/internal", "MOVED", changeStats(129, 123, 2, 8, 15), changeStats(3822, 3824, 165, 413, 414))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4ClassRunner.java", "", "/src/org/junit/internal/runners", "MODIFICATION", changeStats(128, 132, 0, 3, 0), changeStats(3682, 3807, 0, 140, 0))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4MethodRunner.java", "", "/src/org/junit/internal/runners", "NEW", changeStats(0, 125, 125, 0, 0), changeStats(0, 3316, 3316, 0, 0))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethod.java", "", "/src/org/junit/internal/runners", "MODIFICATION", changeStats(157, 64, 0, 26, 84), changeStats(4102, 1582, 0, 809, 2233))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheories.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", changeStats(19, 19, 0, 2, 0), changeStats(514, 530, 0, 96, 0))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheoryMethod.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", changeStats(55, 55, 0, 2, 0), changeStats(1698, 1710, 0, 118, 0))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethodInterfaceTest.java", "", "/src/org/junit/tests/extension", "MODIFICATION", changeStats(34, 34, 0, 2, 0), changeStats(814, 838, 0, 109, 0)))
		])
	}

	@Test "should read file events with additional custom attributes"() {
		def countChangeSizeInLines = false
		def findAmountOfJUnitImports = { context ->
			String textBefore = context.change.beforeRevision?.content
			textBefore == null ? 0 : textBefore.findAll("import org.junit").size()
		}
		def commitMunger = new CommitFilesMunger(jUnitProject, countChangeSizeInLines, [findAmountOfJUnitImports])

		def changeEvents = readChangeEvents(fromDate, toDate, jUnitProject, commitMunger)

		assert asString(changeEvents) == asString([
				fileChangeEvent(commitInfo, fileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", "MODIFICATION", NA, NA), [4]),
				fileChangeEvent(commitInfo, fileChangeInfo("TheoryMethod.java", "TheoryMethodRunner.java", "/src/org/junit/experimental/theories/internal", "/src/org/junit/experimental/theories/internal", "MOVED", NA, NA), [9]),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4ClassRunner.java", "", "/src/org/junit/internal/runners", "MODIFICATION", NA, NA), [8]),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4MethodRunner.java", "", "/src/org/junit/internal/runners", "NEW", NA, NA), [0]),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethod.java", "", "/src/org/junit/internal/runners", "MODIFICATION", NA, NA), [6]),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheories.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", NA, NA), [3]),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheoryMethod.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", NA, NA), [5]),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethodInterfaceTest.java", "", "/src/org/junit/tests/extension", "MODIFICATION", NA, NA), [5])
		])
	}


	private static List readChangeEvents(fromDate, toDate, project, commitMunger) {
		def eventsConsumer = new EventConsumer()
		def eventsReader = new ChangeEventsReader(new CommitReader(project, 1), commitMunger.&mungeCommit)
		eventsReader.readPresentToPast(fromDate, toDate, eventsConsumer.consume)
		eventsConsumer.changeEvents
	}

	private static asString(Collection collection) {
		collection.join(",\n")
	}
	private static fileChangeEvent(commitInfo, fileChangeInfo, additionalAttributes = []) {
		new FileChangeEvent(commitInfo, fileChangeInfo, additionalAttributes)
	}
	private static fileChangeInfo(fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType, lines, chars) {
		new FileChangeInfo(fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType, lines, chars)
	}
	private static changeStats(before, after, added, modified, removed) {
		new ChangeStats(before, after, added, modified, removed)
	}

	private static class EventConsumer {
		List changeEvents
		Closure consume = { List changeEvents -> this.changeEvents = changeEvents }
	}

	private final Date fromDate = dateTime("14:40 03/10/2007")
	private final Date toDate = dateTime("14:45 03/10/2007")
	private final commitComment = "Rename TestMethod -> JUnit4MethodRunner Rename methods in JUnit4MethodRunner to make run order clear"
	private final commitInfo = new CommitInfo("43b0fe352d5bced0c341640d0c630d23f2022a7e", "dsaff <dsaff>", exactDateTime("14:42:16 03/10/2007"), commitComment)
	private final Project jUnitProject = findJUnitProject()
}
