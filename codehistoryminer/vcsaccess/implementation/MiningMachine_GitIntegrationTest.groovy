package codehistoryminer.vcsaccess.implementation
import codehistoryminer.core.common.events.CommitInfo
import codehistoryminer.core.common.events.FileChangeEvent
import codehistoryminer.core.common.events.FileChangeInfo
import codehistoryminer.core.common.langutil.Cancelled
import codehistoryminer.core.common.langutil.Date
import codehistoryminer.core.common.langutil.DateRange
import codehistoryminer.core.vcs.CommitProgressIndicator
import codehistoryminer.core.vcs.FileChangeEventMiner
import codehistoryminer.core.vcs.LineAndCharChangeMiner
import codehistoryminer.core.vcs.MiningMachine
import codehistoryminer.core.vcs.filetype.FileTypes
import codehistoryminer.vcsaccess.VcsActionsLog
import codehistoryminer.vcsaccess.implementation.wrappers.VcsProjectWrapper
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot
import liveplugin.PluginUtil
import org.junit.Test
import vcsreader.Change
import vcsreader.vcs.commandlistener.VcsCommand

import static codehistoryminer.core.common.events.ChangeStats.*
import static codehistoryminer.core.common.langutil.DateTimeTestUtil.*
import static codehistoryminer.vcsaccess.VcsActions.commonVcsRootsAncestor
import static codehistoryminer.vcsaccess.VcsActions.vcsRootsIn
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class MiningMachine_GitIntegrationTest {

    @Test void "read file events"() {
		def countChangeSizeInLines = false
		def changeEvents = readChangeEvents(date("03/10/2007"), date("04/10/2007"), jUnitProject, countChangeSizeInLines)
                .findAll{ it.commitTime == commitInfo.commitTime }

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

	@Test void "read file events with change size details"() {
		def countChangeSizeInLines = true
		def changeEvents = readChangeEvents(date("03/10/2007"), date("04/10/2007"), jUnitProject, countChangeSizeInLines)
                .findAll{ it.commitTime == commitInfo.commitTime }

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

	@Test void "ignore change size details for binary files"() {
        def countChangeSizeInLines = true
        def changeEvents = readChangeEvents(date("15/07/2012"), date("16/07/2012"), jUnitProject, countChangeSizeInLines)
                .findAll { it.fileName.contains(".jar") || it.fileNameBefore.contains(".jar") }

        assertThat(asString(changeEvents), equalTo(asString([
                fileChangeEvent(commitInfo2, fileChangeInfo("hamcrest-core-1.3.0RC2.jar", "", "/lib", "", "DELETED"), linesStatsNA() + charsStatsNA()),
                fileChangeEvent(commitInfo2, fileChangeInfo("", "hamcrest-core-1.3.jar", "", "/lib", "NEW"), linesStatsNA() + charsStatsNA()),
        ])))
    }

	@Test void "merged commits are skipped because change events create from original commits"() {
        def countChangeSizeInLines = false
        def changeEvents = readChangeEvents(date("11/04/2014"), date("14/04/2014"), jUnitProject, countChangeSizeInLines)

        assertThat(asString(changeEvents), equalTo(asString([
                fileChangeEvent(commitInfo3, fileChangeInfo("", "ErrorReportingRunner.java", "", "/src/main/java/org/junit/internal/runners", "MODIFICATION")),
                fileChangeEvent(commitInfo3, fileChangeInfo("", "ErrorReportingRunnerTest.java", "", "/src/test/java/org/junit/tests/internal/runners", "NEW"))
        ])))
    }

	private static List<FileChangeEvent> readChangeEvents(Date fromDate, Date toDate, Project project, boolean countChangeSizeInLines) {
		def fileTypes = new FileTypes([]) {
			@Override boolean isBinary(String fileName) {
				FileTypeManager.instance.getFileTypeByFileName(fileName).binary
			}
		}
		def miners = countChangeSizeInLines ?
				[new FileChangeEventMiner(UTC), new LineAndCharChangeMiner(fileTypes, miningMachineListener)] :
				[new FileChangeEventMiner(UTC)]
        def vcsProject = new VcsProjectWrapper(project, vcsRootsIn(project), commonVcsRootsAncestor(project), vcsActionsLog)
		def config = new MiningMachine.Config(miners, fileTypes, UTC).withListener(miningMachineListener).withCacheFileContent(false)
		new MiningMachine(config)
				.mine(vcsProject, [new DateRange(fromDate, toDate)], Cancelled.never)
				.collectMany{ it.fileChangeEvents }
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
	private final commitInfo = new CommitInfo("43b0fe352d5bced0c341640d0c630d23f2022a7e", "dsaff <dsaff>", time("14:42:16 03/10/2007"), commitComment)
    private final commitComment2 = "Update Hamcrest from 1.3.RC2 to 1.3"
    private final commitInfo2 = new CommitInfo("40375ef1fc08b1f666b21d299d8b52b10a53e6fb", "Marc Philipp", time("12:03:49 15/07/2012"), commitComment2)
    private final commitComment3 = "fixes #177\n\nnull check for test class in ErrorReportingRunner"
    private final commitInfo3 = new CommitInfo("96cfed79612de559e454a1a91724a061e8615ae4", "Alexander Jipa", time("19:11:40 11/04/2014"), commitComment3)

	private final Project jUnitProject = IJCommitReaderGitTest.findOpenedJUnitProject()

	private final static miningMachineListener = new MiningMachine.Listener() {
		@Override void onVcsError(String error) { PluginUtil.show(error) }
		@Override void onException(Exception e) { PluginUtil.show(e) }
		@Override void onUpdate(CommitProgressIndicator indicator) {}
		@Override void failedToMine(Change change, String description, Throwable throwable) { PluginUtil.show(throwable) }
		@Override void beforeCommand(VcsCommand command) {}
		@Override void afterCommand(VcsCommand command) {}
	}

	private final static vcsActionsLog = new VcsActionsLog() {
		@Override def errorReadingCommits(Exception e, Date fromDate, Date toDate) {
			PluginUtil.show(e)
		}
		@Override def errorReadingCommits(String error) {
			PluginUtil.show(error)
		}
		@Override def onExtractChangeEventException(Throwable t) {
			PluginUtil.show(t)
		}
		@Override def failedToMine(String message) {
			PluginUtil.show(message)
		}
		@Override def failedToLocate(VcsRoot vcsRoot, Project project) {}
	}
}
