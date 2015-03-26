package codemining.vcsaccess.implementation
import codemining.core.common.events.CommitInfo
import codemining.core.common.events.FileChangeEvent
import codemining.core.common.events.FileChangeInfo
import codemining.core.common.langutil.DateRange
import codemining.core.vcs.*
import codemining.core.vcs.filetype.FileTypes
import codemining.vcsaccess.VcsActionsLog
import codemining.vcsaccess.implementation.wrappers.VcsProjectWrapper
import com.intellij.notification.NotificationType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot
import liveplugin.PluginUtil
import org.junit.Test
import vcsreader.Change
import vcsreader.Commit

import static codemining.core.common.events.ChangeStats.*
import static codemining.core.common.langutil.DateTimeUtil.*
import static codemining.vcsaccess.VcsActions.commonVcsRootsAncestor
import static codemining.vcsaccess.VcsActions.vcsRootsIn
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class MiningCommitReader_GitIntegrationTest {

    @Test void "should read file events"() {
		def countChangeSizeInLines = false
		def commitMiners = createCommitMiners(countChangeSizeInLines)

		def changeEvents = readChangeEvents(date("03/10/2007"), date("04/10/2007"), jUnitProject, commitMiners)
                .findAll{ it.revisionDate == commitInfo.revisionDate }

        assertThat(asString(changeEvents), equalTo(asString([
				fileChangeEvent(commitInfo, fileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", "MODIFICATION")),
				fileChangeEvent(commitInfo, fileChangeInfo("TheoryMethod.java", "TheoryMethodRunner.java", "/src/org/junit/experimental/theories/internal", "/src/org/junit/experimental/theories/internal", "MOVED")),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4ClassRunner.java", "", "/src/org/junit/internal/runners", "MODIFICATION")),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4MethodRunner.java", "", "/src/org/junit/internal/runners", "NEW")),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethod.java", "", "/src/org/junit/internal/runners", "MODIFICATION")),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheories.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION")),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheoryMethod.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION")),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethodInterfaceTest.java", "", "/src/org/junit/tests/extension", "MODIFICATION"))
		])))
	}

	@Test void "should read file events with change size details"() {
		def countChangeSizeInLines = true
		def commitMiners = createCommitMiners(countChangeSizeInLines)

		def changeEvents = readChangeEvents(date("03/10/2007"), date("04/10/2007"), jUnitProject, commitMiners)
                .findAll{ it.revisionDate == commitInfo.revisionDate }

		assertThat(asString(changeEvents), equalTo(asString([
				fileChangeEvent(commitInfo, fileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", "MODIFICATION"), linesStats(37, 37, 0, 4, 0) + charsStats(950, 978, 0, 215, 0)),
				fileChangeEvent(commitInfo, fileChangeInfo("TheoryMethod.java", "TheoryMethodRunner.java", "/src/org/junit/experimental/theories/internal", "/src/org/junit/experimental/theories/internal", "MOVED"), linesStats(129, 123, 2, 8, 15) + charsStats(3822, 3824, 165, 413, 414)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4ClassRunner.java", "", "/src/org/junit/internal/runners", "MODIFICATION"), linesStats(128, 132, 0, 3, 0) + charsStats(3682, 3807, 0, 140, 0)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "JUnit4MethodRunner.java", "", "/src/org/junit/internal/runners", "NEW"), linesStats(0, 125, 125, 0, 0) + charsStats(0, 3316, 3316, 0, 0)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethod.java", "", "/src/org/junit/internal/runners", "MODIFICATION"), linesStats(157, 64, 0, 26, 84) + charsStats(4102, 1582, 0, 809, 2233)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheories.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION"), linesStats(19, 19, 0, 2, 0) + charsStats(514, 530, 0, 96, 0)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "StubbedTheoryMethod.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION"), linesStats(55, 55, 0, 2, 0) + charsStats(1698, 1710, 0, 118, 0)),
				fileChangeEvent(commitInfo, fileChangeInfo("", "TestMethodInterfaceTest.java", "", "/src/org/junit/tests/extension", "MODIFICATION"), linesStats(34, 34, 0, 2, 0) + charsStats(814, 838, 0, 109, 0))
		])))
	}

    @Test void "should ignore change size details for binary files"() {
        def countChangeSizeInLines = true
        def commitMiners = createCommitMiners(countChangeSizeInLines)

        def changeEvents = readChangeEvents(date("15/07/2012"), date("16/07/2012"), jUnitProject, commitMiners)
                .findAll { it.fileName.contains(".jar") || it.fileNameBefore.contains(".jar") }

        assertThat(asString(changeEvents), equalTo(asString([
                fileChangeEvent(commitInfo2, fileChangeInfo("hamcrest-core-1.3.0RC2.jar", "", "/lib", "", "DELETED"), linesStatsNA() + charsStatsNA()),
                fileChangeEvent(commitInfo2, fileChangeInfo("", "hamcrest-core-1.3.jar", "", "/lib", "NEW"), linesStatsNA() + charsStatsNA()),
        ])))
    }

    @Test void "merged commits are skipped because change events create from original commits"() {
        def countChangeSizeInLines = false
        def commitMiners = createCommitMiners(countChangeSizeInLines)

        def changeEvents = readChangeEvents(date("11/04/2014"), date("14/04/2014"), jUnitProject, commitMiners)

        assertThat(asString(changeEvents), equalTo(asString([
                fileChangeEvent(commitInfo3, fileChangeInfo("", "ErrorReportingRunner.java", "", "/src/main/java/org/junit/internal/runners", "MODIFICATION")),
                fileChangeEvent(commitInfo3, fileChangeInfo("", "ErrorReportingRunnerTest.java", "", "/src/test/java/org/junit/tests/internal/runners", "NEW"))
        ])))
    }

	private static List<MainFileMiner> createCommitMiners(boolean countChangeSizeInLines) {
		def fileTypes = new FileTypes([]) {
			@Override boolean isBinary(String fileName) {
				FileTypeManager.instance.getFileTypeByFileName(fileName).binary
			}
		}
		countChangeSizeInLines ?
			[new MainFileMiner(), new LineAndCharChangeMiner(fileTypes, listener)] :
			[new MainFileMiner()]
	}

	private static List<FileChangeEvent> readChangeEvents(Date fromDate, Date toDate, Project project, List<MainFileMiner> miners) {
        def projectWrapper = new VcsProjectWrapper(project, vcsRootsIn(project), commonVcsRootsAncestor(project), vcsActionsLog)
		def commitReader = new MiningCommitReader(new CommitReader(projectWrapper, CommitReaderConfig.noCachingDefaults), miners, commitReaderListener)

		commitReader.readCommits(dateRange(fromDate, toDate)).collectMany { MinedCommit minedCommit ->
			minedCommit.fileChangeEvents
		}
	}

	private static asString(Collection collection) {
		collection.join(",\n")
	}
	private static fileChangeEvent(commitInfo, fileChangeInfo, additionalAttributes = [:]) {
		new FileChangeEvent(commitInfo, fileChangeInfo, additionalAttributes)
	}
	private static fileChangeInfo(fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType) {
		new FileChangeInfo(fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType)
	}


	private final commitComment = "Rename TestMethod -> JUnit4MethodRunner Rename methods in JUnit4MethodRunner to make run order clear"
	private final commitInfo = new CommitInfo("43b0fe352d5bced0c341640d0c630d23f2022a7e", "dsaff <dsaff>", dateTime("14:42:16 03/10/2007"), commitComment)
    private final commitComment2 = "Update Hamcrest from 1.3.RC2 to 1.3"
    private final commitInfo2 = new CommitInfo("40375ef1fc08b1f666b21d299d8b52b10a53e6fb", "Marc Philipp", dateTime("12:03:49 15/07/2012"), commitComment2)
    private final commitComment3 = "fixes #177\n\nnull check for test class in ErrorReportingRunner"
    private final commitInfo3 = new CommitInfo("96cfed79612de559e454a1a91724a061e8615ae4", "Alexander Jipa", dateTime("19:11:40 11/04/2014"), commitComment3)

	private final Project jUnitProject = IJCommitReaderGitTest.findOpenedJUnitProject()

    private static final listener = new NoFileContentListener() {
        @Override void failedToLoadContent(Change change) {
            throw new IllegalStateException("Failed to process: ${change}")
        }
    }

	private final static commitReaderListener = new MiningCommitReaderListener() {
		@Override void errorReadingCommits(String error) {
			PluginUtil.show(error, "", NotificationType.ERROR)
		}
		@Override void onExtractChangeEventException(Exception e) {
			PluginUtil.show(e)
		}
		@Override void afterMiningCommit(Commit commit) {}
		@Override void onCurrentDateRange(DateRange dateRange) {}
		@Override void beforeMiningCommit(Commit commit) {}
	}

	private final static vcsActionsLog = new VcsActionsLog() {
		@Override def errorReadingCommits(Exception e, Date fromDate, Date toDate) {
			PluginUtil.show(e)
		}
		@Override def errorReadingCommits(String error) {
			PluginUtil.show(error)
		}
		@Override def onExtractChangeEventException(Exception e) {
			PluginUtil.show(e)
		}
		@Override def failedToLoadContent(String message) {
			PluginUtil.show(message)
		}
		@Override def failedToLocate(VcsRoot vcsRoot, Project project) {}
	}
}
