package historyreader
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware
import org.junit.Test

import static util.DateTimeUtil.dateTime

class CommitReaderGitTest {
	private final Project jUnitProject = findJUnitProject()

	@Test "should interpret renamed file as a single event"() {
		def commit = readSingleCommit("43b0fe3", dateTime("15:40 03/10/2007"), dateTime("15:45 03/10/2007"))
		def change = commit.changes.find{ it.beforeRevision.file.name.contains("TheoryMethod") }

		assert change.type == Change.Type.MOVED
		assert change.beforeRevision.file.name == "TheoryMethod.java"
		assert change.afterRevision.file.name == "TheoryMethodRunner.java"
	}

	@Test "should interpret moved file as a single event"() {
		def commit = readSingleCommit("a19e98f", dateTime("08:50 28/07/2011"), dateTime("09:00 28/07/2011"))
		def change = commit.changes.find{ it.beforeRevision.file.name.contains("RuleFieldValidator") }

		assert change.type == Change.Type.MOVED
		assert change.beforeRevision.file.name == "RuleFieldValidator.java"
		assert change.afterRevision.file.name == "RuleFieldValidator.java"
	}

	@Test "should ignore merge commits and include merge changes as separate change lists"() {
		def commits = readSingleCommit("dc730e3", dateTime("10:00 09/05/2013"), dateTime("17:02 09/05/2013"))

		def changes = commits.changes
		assert changes.first().beforeRevision.file.name == "ComparisonFailureTest.java"
	}

	private Commit readSingleCommit(String expectedGitHash, Date from, Date to) {
		def commits = new CommitReader(jUnitProject).readCommits(from, to).toList()
		assert commits.size() == 1 : "Excpected single change list but got ${commits.size()}"

		def commit = commits.first()
		(commit as VcsRevisionNumberAware ).revisionNumber.asString().with {
			assert it.startsWith(expectedGitHash) : "Expected hash $expectedGitHash but got $it"
		}
		commit
	}

	static Project findJUnitProject() {
		def jUnitProject = findProject("junit")

		def sourceRoots = ProjectRootManager.getInstance(jUnitProject).contentSourceRoots.toList()
		def vcsRoot = ProjectLevelVcsManager.getInstance(jUnitProject).getVcsRootObjectFor(sourceRoots.first())
		assert vcsRoot.vcs.class.simpleName == "GitVcs"

		jUnitProject
	}

	static Project findProject(String projectName) {
		def project = ProjectManager.instance.openProjects.find{ it.name == projectName }
		assert project != null: "Couldn't find open '$projectName' project"
		project
	}
}
