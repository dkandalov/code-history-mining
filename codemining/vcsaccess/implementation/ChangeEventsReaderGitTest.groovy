package codemining.vcsaccess.implementation
import codemining.core.common.events.ChangeStats
import codemining.core.common.events.CommitInfo
import codemining.core.common.events.FileChangeEvent
import codemining.core.common.events.FileChangeInfo
import codemining.core.vcs.CommitMunger
import codemining.core.vcs.CommitMungerListener
import com.intellij.openapi.project.Project
import org.junit.Test
import codemining.vcsaccess.ChangeEventsReader
import codemining.vcsaccess.implementation.wrappers.VcsProjectWrapper
import vcsreader.Change

import static codemining.core.common.events.ChangeStats.NA
import static codemining.core.common.langutil.DateTimeUtil.date
import static codemining.core.common.langutil.DateTimeUtil.dateTime
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat
import static codemining.vcsaccess.VcsAccess.commonVcsRootsAncestor
import static codemining.vcsaccess.VcsAccess.vcsRootsIn

class ChangeEventsReaderGitTest {

    @Test void "should read file events"() {
		def countChangeSizeInLines = false
		def commitMunger = new CommitMunger(countChangeSizeInLines, listener)

		def changeEvents = readChangeEvents(date("03/10/2007"), date("03/10/2007"), jUnitProject, commitMunger)
                .findAll{ it.revisionDate == commitInfo.revisionDate }

        assertThat(asString(changeEvents), equalTo(asString([
				fileChangeEvent(commitInfo, fileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", "MODIFICATION", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("TheoryMethod.java", "TheoryMethodRunner.java", "/src/org/junit/experimental/theories/internal", "/src/org/junit/experimental/theories/internal", "MOVED", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4ClassRunner.java", "", "/src/org/junit/internal/runners", "MODIFICATION", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4MethodRunner.java", "", "/src/org/junit/internal/runners", "NEW", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethod.java", "", "/src/org/junit/internal/runners", "MODIFICATION", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheories.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheoryMethod.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", NA, NA)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethodInterfaceTest.java", "", "/src/org/junit/tests/extension", "MODIFICATION", NA, NA))
		])))
	}

	@Test void "should read file events with change size details"() {
		def countChangeSizeInLines = true
		def commitMunger = new CommitMunger(countChangeSizeInLines, listener)

		def changeEvents = readChangeEvents(date("03/10/2007"), date("03/10/2007"), jUnitProject, commitMunger)
                .findAll{ it.revisionDate == commitInfo.revisionDate }

		assertThat(asString(changeEvents), equalTo(asString([
				fileChangeEvent(commitInfo, fileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", "MODIFICATION", changeStats(37, 37, 0, 4, 0), changeStats(950, 978, 0, 215, 0))),
				fileChangeEvent(commitInfo, fileChangeInfo("TheoryMethod.java", "TheoryMethodRunner.java", "/src/org/junit/experimental/theories/internal", "/src/org/junit/experimental/theories/internal", "MOVED", changeStats(129, 123, 2, 8, 15), changeStats(3822, 3824, 165, 413, 414))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4ClassRunner.java", "", "/src/org/junit/internal/runners", "MODIFICATION", changeStats(128, 132, 0, 3, 0), changeStats(3682, 3807, 0, 140, 0))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4MethodRunner.java", "", "/src/org/junit/internal/runners", "NEW", changeStats(0, 125, 125, 0, 0), changeStats(0, 3316, 3316, 0, 0))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethod.java", "", "/src/org/junit/internal/runners", "MODIFICATION", changeStats(157, 64, 0, 26, 84), changeStats(4102, 1582, 0, 809, 2233))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheories.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", changeStats(19, 19, 0, 2, 0), changeStats(514, 530, 0, 96, 0))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheoryMethod.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", changeStats(55, 55, 0, 2, 0), changeStats(1698, 1710, 0, 118, 0))),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethodInterfaceTest.java", "", "/src/org/junit/tests/extension", "MODIFICATION", changeStats(34, 34, 0, 2, 0), changeStats(814, 838, 0, 109, 0)))
		])))
	}

    @Test void "should ignore change size details for binary files"() {
        def countChangeSizeInLines = true
        def commitMunger = new CommitMunger(countChangeSizeInLines, listener)

        def changeEvents = readChangeEvents(date("15/07/2012"), date("15/07/2012"), jUnitProject, commitMunger)
                .findAll { it.fileName.contains(".jar") || it.fileNameBefore.contains(".jar") }

        assertThat(asString(changeEvents), equalTo(asString([
                fileChangeEvent(commitInfo2, fileChangeInfo("hamcrest-core-1.3.0RC2.jar", "", "/lib", "", "DELETED", NA, NA)),
                fileChangeEvent(commitInfo2, fileChangeInfo("", "hamcrest-core-1.3.jar", "", "/lib", "NEW", NA, NA)),
        ])))
    }

    @Test void "merged commits are skipped because change events create from original commits"() {
        def countChangeSizeInLines = false
        def commitMunger = new CommitMunger(countChangeSizeInLines, listener)

        def changeEvents = readChangeEvents(date("11/04/2014"), date("13/04/2014"), jUnitProject, commitMunger)

        assertThat(asString(changeEvents), equalTo(asString([
                fileChangeEvent(commitInfo3, fileChangeInfo("", "ErrorReportingRunner.java", "", "/src/main/java/org/junit/internal/runners", "MODIFICATION", NA, NA)),
                fileChangeEvent(commitInfo3, fileChangeInfo("", "ErrorReportingRunnerTest.java", "", "/src/test/java/org/junit/tests/internal/runners", "NEW", NA, NA))
        ])))
    }


    private static List<FileChangeEvent> readChangeEvents(Date fromDate, Date toDate, Project project, CommitMunger commitMunger) {
		def eventsConsumer = new EventConsumer()
        def projectWrapper = new VcsProjectWrapper(project, vcsRootsIn(project), commonVcsRootsAncestor(project))
        def eventsReader = new ChangeEventsReader(projectWrapper, commitMunger, null)

		eventsReader.readPresentToPast(fromDate, toDate, eventsConsumer.consume)

		eventsConsumer.changeEvents
	}

	private static asString(Collection collection) {
		collection.join(",\n")
	}
	private static fileChangeEvent(commitInfo, fileChangeInfo, additionalAttributes = [:]) {
		new FileChangeEvent(commitInfo, fileChangeInfo, additionalAttributes)
	}
	private static fileChangeInfo(fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType, lines, chars) {
		new FileChangeInfo(fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType, lines, chars)
	}
	private static changeStats(before, after, added, modified, removed) {
		new ChangeStats(before, after, added, modified, removed)
	}

	private static class EventConsumer {
		List changeEvents = []
		Closure consume = { List changeEvents -> this.changeEvents.addAll(changeEvents) }
	}

	private final commitComment = "Rename TestMethod -> JUnit4MethodRunner Rename methods in JUnit4MethodRunner to make run order clear"
	private final commitInfo = new CommitInfo("43b0fe352d5bced0c341640d0c630d23f2022a7e", "dsaff <dsaff>", dateTime("14:42:16 03/10/2007"), commitComment)
    private final commitComment2 = "Update Hamcrest from 1.3.RC2 to 1.3"
    private final commitInfo2 = new CommitInfo("40375ef1fc08b1f666b21d299d8b52b10a53e6fb", "Marc Philipp <mail@marcphilipp.de>", dateTime("12:03:49 15/07/2012"), commitComment2)
    private final commitComment3 = "fixes #177\n\nnull check for test class in ErrorReportingRunner"
    private final commitInfo3 = new CommitInfo("96cfed79612de559e454a1a91724a061e8615ae4", "Alexander Jipa <alexander.jipa@gmail.com>", dateTime("19:11:40 11/04/2014"), commitComment3)

	private final Project jUnitProject = CommitReaderGitTest.findOpenedJUnitProject()

    private final listener = new CommitMungerListener() {
        @Override void failedToLoadContent(Change change) {
            throw new IllegalStateException("Failed to process: ${change}")
        }
    }
}
