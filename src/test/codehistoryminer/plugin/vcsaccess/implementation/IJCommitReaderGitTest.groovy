package codehistoryminer.plugin.vcsaccess.implementation
import codehistoryminer.core.common.langutil.Date
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import com.intellij.openapi.vcs.versionBrowser.VcsRevisionNumberAware
import org.junit.Test

import static codehistoryminer.core.common.langutil.DateTimeTestUtil.date
import static codehistoryminer.plugin.vcsaccess.VcsActions.vcsRootsIn

class IJCommitReaderGitTest {

	@Test void "renamed file is interpreted as a single event"() {
		def commit = readSingleCommit("43b0fe3", date("03/10/2007"), date("04/10/2007"))
		def change = commit.changes.find{ it.beforeRevision.file.name.contains("TheoryMethod") }

		assert change.type == Change.Type.MOVED
		assert change.beforeRevision.file.name == "TheoryMethod.java"
		assert change.afterRevision.file.name == "TheoryMethodRunner.java"
	}

	@Test void "moved file is interpreted as a single event"() {
		def commit = readSingleCommit("a19e98f", date("28/07/2011"), date("29/07/2011"))
		def change = commit.changes.find{ it.beforeRevision.file.name.contains("RuleFieldValidator") }

		assert change.type == Change.Type.MOVED
		assert change.beforeRevision.file.name == "RuleFieldValidator.java"
		assert change.afterRevision.file.name == "RuleFieldValidator.java"
		assert change.beforeRevision.file.path.endsWith("src/main/java/org/junit/rules/RuleFieldValidator.java")
		assert change.afterRevision.file.path.endsWith("src/main/java/org/junit/internal/runners/rules/RuleFieldValidator.java")
	}

	@Test void "should ignore merge commits and include merge changes as separate change lists"() {
		def commits = readSingleCommit("dc730e3", date("09/05/2013"), date("10/05/2013"))

        assert commits.changes.first().beforeRevision.file.name == "ComparisonFailureTest.java"
	}

	@Test void "end date is exclusive"() {
		def commits = readJUnitCommits(date("08/10/2007"), date("09/10/2007"))
		assert commits.size() == 3
		commits = readJUnitCommits(date("08/10/2007"), date("10/10/2007"))
		assert commits.size() == 7
	}

	private Commit readSingleCommit(String gitHash, Date from, Date to) {
		def commits = readJUnitCommits(from, to)
				.findAll{ (it as VcsRevisionNumberAware).revisionNumber.asString().startsWith(gitHash) }

		assert commits.size() == 1 : "Expected single element but got ${commits.size()} commits for dates from [${from}] to [${to}]"
		commits.first()
	}

	private List<CommittedChangeList> readJUnitCommits(Date from, Date to) {
		new IJCommitReader(jUnitProject).readCommits(from, to, vcsRootsIn(jUnitProject))
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
