package vcsaccess.implementation
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware
import org.junit.Test

import static util.DateTimeUtil.*
import static vcsaccess.ChangeEventsReader.vcsRootsIn

class CommitReaderGitTest {

	@Test "should interpret renamed file as a single event"() {
		def commit = readSingleCommit("43b0fe3", dateTime("14:40 03/10/2007"), dateTime("14:45 03/10/2007"))
		def change = commit.changes.find{ it.beforeRevision.file.name.contains("TheoryMethod") }

		assert change.type == Change.Type.MOVED
		assert change.beforeRevision.file.name == "TheoryMethod.java"
		assert change.afterRevision.file.name == "TheoryMethodRunner.java"
	}

	@Test "should interpret moved file as a single event"() {
		def commit = readSingleCommit("a19e98f", dateTime("07:50 28/07/2011"), dateTime("08:00 28/07/2011"))
		def change = commit.changes.find{ it.beforeRevision.file.name.contains("RuleFieldValidator") }

		assert change.type == Change.Type.MOVED
		assert change.beforeRevision.file.name == "RuleFieldValidator.java"
		assert change.afterRevision.file.name == "RuleFieldValidator.java"
	}

	@Test "should ignore merge commits and include merge changes as separate change lists"() {
		def commits = readSingleCommit("dc730e3", dateTime("09:00 09/05/2013"), dateTime("16:02 09/05/2013"))

		def changes = commits.changes
		assert changes.first().beforeRevision.file.name == "ComparisonFailureTest.java"
	}

	@Test "should order commits by ascending time when reading from past to present"() {
		def isReadingPresentToPast = false
		def from = date("03/10/2007")
		def to = date("04/10/2007")

		def commits = readJUnitCommits(from, to, isReadingPresentToPast)

		assert commits.size() == 3
		assert commits[0].commitDate.before(commits[1].commitDate)
		assert commits[1].commitDate.before(commits[2].commitDate)
	}

	@Test "should order commits by descending time when reading from present to past"() {
		def isReadingPresentToPast = true
		def from = date("03/10/2007")
		def to = date("04/10/2007")

		def commits = readJUnitCommits(from, to, isReadingPresentToPast)

		assert commits.size() == 3
		assert commits[0].commitDate.after(commits[1].commitDate)
		assert commits[1].commitDate.after(commits[2].commitDate)
	}

	@Test "end date is exclusive when reading present to past"() {
		def isReadingPresentToPast = true

		def commits = readJUnitCommits(date("08/10/2007"), date("09/10/2007"), isReadingPresentToPast)
		assert commits.size() == 3
		commits = readJUnitCommits(date("08/10/2007"), date("10/10/2007"), isReadingPresentToPast)
		assert commits.size() == 7
	}

	@Test "end date is exclusive when reading past to present"() {
		def isReadingPresentToPast = false

		def commits = readJUnitCommits(date("08/10/2007"), date("09/10/2007"), isReadingPresentToPast)
		assert commits.size() == 3
		commits = readJUnitCommits(date("08/10/2007"), date("10/10/2007"), isReadingPresentToPast)
		assert commits.size() == 7
	}

	private Commit readSingleCommit(String gitHash, Date from, Date to) {
		def commits = readJUnitCommits(from, to, true)
				.findAll{ (it as VcsRevisionNumberAware).revisionNumber.asString().startsWith(gitHash) }

		assert commits.size() == 1 : "Expected single element but got ${commits.size()} commits for dates from [${from}] to [${to}]"
		commits.first()
	}

	private List<CommittedChangeList> readJUnitCommits(Date from, Date to, boolean isReadingPresentToPast) {
		new CommitReader(jUnitProject).readCommits(from, to, isReadingPresentToPast, vcsRootsIn(jUnitProject))
				.toList().findAll{it != CommitReader.NO_MORE_COMMITS}
	}

	static Project findOpenedJUnitProject() {
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

	private final Project jUnitProject = findOpenedJUnitProject()
}
