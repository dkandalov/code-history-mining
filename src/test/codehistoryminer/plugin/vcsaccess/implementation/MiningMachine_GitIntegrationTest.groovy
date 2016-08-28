package codehistoryminer.plugin.vcsaccess.implementation

import codehistoryminer.core.lang.DateRange
import codehistoryminer.core.miner.MiningMachine
import codehistoryminer.core.miner.filechange.CommitInfo
import codehistoryminer.core.miner.filechange.FileChangeInfo
import codehistoryminer.core.miner.filechange.FileChangeMiner
import codehistoryminer.core.miner.linchangecount.LineAndCharChangeMiner
import codehistoryminer.core.vcsreader.CommitProgressIndicator
import codehistoryminer.plugin.vcsaccess.VcsActionsLog
import codehistoryminer.plugin.vcsaccess.implementation.wrappers.VcsProjectWrapper
import codehistoryminer.publicapi.analysis.filechange.FileChange
import codehistoryminer.publicapi.lang.Cancelled
import codehistoryminer.publicapi.lang.Date
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot
import liveplugin.PluginUtil
import org.junit.Test
import org.vcsreader.VcsChange
import org.vcsreader.vcs.VcsCommand

import static codehistoryminer.core.lang.DateTimeTestUtil.*
import static codehistoryminer.plugin.vcsaccess.VcsActions.commonVcsRootsAncestor
import static codehistoryminer.plugin.vcsaccess.VcsActions.vcsRootsIn
import static codehistoryminer.publicapi.analysis.filechange.ChangeType.*
import static codehistoryminer.publicapi.analysis.linechangecount.ChangeStats.*
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class MiningMachine_GitIntegrationTest {

    @Test void "read file change data"() {
		def countChangeSizeInLines = false
		def changes = readChanges(date("03/10/2007"), date("04/10/2007"), jUnitProject, countChangeSizeInLines)
                .findAll{ it.commitTime == commitInfo.commitTime }

        assertThat(asString(changes), equalTo(asString([
		        fileChange(commitInfo, fileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", MODIFIED)),
		        fileChange(commitInfo, fileChangeInfo("TheoryMethod.java", "TheoryMethodRunner.java", "/src/org/junit/experimental/theories/internal", "/src/org/junit/experimental/theories/internal", MOVED)),
		        fileChange(commitInfo, fileChangeInfo("", "JUnit4ClassRunner.java", "", "/src/org/junit/internal/runners", MODIFIED)),
		        fileChange(commitInfo, fileChangeInfo("", "JUnit4MethodRunner.java", "", "/src/org/junit/internal/runners", ADDED)),
		        fileChange(commitInfo, fileChangeInfo("", "TestMethod.java", "", "/src/org/junit/internal/runners", MODIFIED)),
		        fileChange(commitInfo, fileChangeInfo("", "StubbedTheories.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", MODIFIED)),
		        fileChange(commitInfo, fileChangeInfo("", "StubbedTheoryMethod.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", MODIFIED)),
		        fileChange(commitInfo, fileChangeInfo("", "TestMethodInterfaceTest.java", "", "/src/org/junit/tests/extension", MODIFIED))
		]*.data)))
	}

	@Test void "read file with change size details"() {
		def countChangeSizeInLines = true
		def changes = readChanges(date("03/10/2007"), date("04/10/2007"), jUnitProject, countChangeSizeInLines)
                .findAll{ it.commitTime == commitInfo.commitTime }

		assertThat(asString(changes), equalTo(asString([
				fileChange(commitInfo, fileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", MODIFIED), linesStats(37, 37, 0, 4, 0) + charsStats(950, 978, 0, 215, 0)),
				fileChange(commitInfo, fileChangeInfo("TheoryMethod.java", "TheoryMethodRunner.java", "/src/org/junit/experimental/theories/internal", "/src/org/junit/experimental/theories/internal", MOVED), linesStats(129, 123, 2, 8, 15) + charsStats(3822, 3824, 165, 413, 414)),
				fileChange(commitInfo, fileChangeInfo("", "JUnit4ClassRunner.java", "", "/src/org/junit/internal/runners", MODIFIED), linesStats(128, 132, 0, 3, 0) + charsStats(3682, 3807, 0, 140, 0)),
				fileChange(commitInfo, fileChangeInfo("", "JUnit4MethodRunner.java", "", "/src/org/junit/internal/runners", ADDED), linesStats(0, 125, 125, 0, 0) + charsStats(0, 3316, 3316, 0, 0)),
				fileChange(commitInfo, fileChangeInfo("", "TestMethod.java", "", "/src/org/junit/internal/runners", MODIFIED), linesStats(157, 64, 0, 26, 84) + charsStats(4102, 1582, 0, 809, 2233)),
				fileChange(commitInfo, fileChangeInfo("", "StubbedTheories.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", MODIFIED), linesStats(19, 19, 0, 2, 0) + charsStats(514, 530, 0, 96, 0)),
				fileChange(commitInfo, fileChangeInfo("", "StubbedTheoryMethod.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", MODIFIED), linesStats(55, 55, 0, 2, 0) + charsStats(1698, 1710, 0, 118, 0)),
				fileChange(commitInfo, fileChangeInfo("", "TestMethodInterfaceTest.java", "", "/src/org/junit/tests/extension", MODIFIED), linesStats(34, 34, 0, 2, 0) + charsStats(814, 838, 0, 109, 0))
		]*.data)))
	}

	@Test void "ignore change size details for binary files"() {
        def countChangeSizeInLines = true
        def changes = readChanges(date("15/07/2012"), date("16/07/2012"), jUnitProject, countChangeSizeInLines)
                .findAll { it.fileName.contains(".jar") || it.fileNameBefore.contains(".jar") }

        assertThat(asString(changes), equalTo(asString([
		        fileChange(commitInfo2, fileChangeInfo("hamcrest-core-1.3.0RC2.jar", "", "/lib", "", DELETED), statsNA()),
		        fileChange(commitInfo2, fileChangeInfo("", "hamcrest-core-1.3.jar", "", "/lib", ADDED), statsNA()),
        ]*.data)))
    }

	@Test void "merged commits are skipped because change data is created from original commits"() {
        def countChangeSizeInLines = false
        def changes = readChanges(date("11/04/2014"), date("14/04/2014"), jUnitProject, countChangeSizeInLines)

        assertThat(asString(changes), equalTo(asString([
		        fileChange(commitInfo3, fileChangeInfo("", "ErrorReportingRunner.java", "", "/src/main/java/org/junit/internal/runners", MODIFIED)),
		        fileChange(commitInfo3, fileChangeInfo("", "ErrorReportingRunnerTest.java", "", "/src/test/java/org/junit/tests/internal/runners", ADDED))
        ]*.data)))
    }

	private static List<FileChange> readChanges(Date fromDate, Date toDate, Project project, boolean countChangeSizeInLines) {
		def fileTypes = new IJFileTypes()
		def miners = countChangeSizeInLines ?
				[new FileChangeMiner(UTC), new LineAndCharChangeMiner(fileTypes, miningMachineListener)] :
				[new FileChangeMiner(UTC)]
        def vcsProject = new VcsProjectWrapper(project, vcsRootsIn(project), commonVcsRootsAncestor(project), vcsActionsLog)
		def config = new MiningMachine.Config(miners, fileTypes, UTC).withListener(miningMachineListener).withCacheFileContent(false)
		new MiningMachine(config)
				.mine(vcsProject, [new DateRange(fromDate, toDate)], Cancelled.never)
				.collectMany{ it.dataList }
	}

	private static asString(Collection collection) {
		collection.join(",\n")
	}
	private static fileChange(commitInfo, fileChangeInfo, additionalAttributes = [:]) {
		new FileChange(commitInfo, fileChangeInfo, additionalAttributes)
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
		@Override void failedToMine(VcsChange change, String description, Throwable throwable) { PluginUtil.show(throwable) }
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
		@Override def onFailedToMineException(Throwable t) {
			PluginUtil.show(t)
		}
		@Override def failedToMine(String message) {
			PluginUtil.show(message)
		}
		@Override def failedToLocate(VcsRoot vcsRoot, Project project) {}
	}
}
