package history

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.changes.Change
import org.junit.Test

import static history.SourceOfChangeLists.requestChangeListsFor
import static history.util.DateTimeUtil.dateTime

class SourceOfChangeListsGitTest {
	private final Project jUnitProject

	SourceOfChangeListsGitTest() {
		jUnitProject = findJUnitProject()
	}

	@Test "should interpret renamed file as a single event"() {
		def changeLists = requestChangeListsFor(jUnitProject, dateTime("15:40 03/10/2007"), dateTime("15:45 03/10/2007"))
		def change = changeLists.first().changes.find{ it.beforeRevision.file.name.contains("TheoryMethod") }

		assert change.type == Change.Type.MOVED
		assert change.beforeRevision.file.name == "TheoryMethod.java"
		assert change.afterRevision.file.name == "TheoryMethodRunner.java"
	}

	@Test "should interpret moved file as a single event"() {
		def changeLists = requestChangeListsFor(jUnitProject, dateTime("08:50 28/07/2011"), dateTime("09:00 28/07/2011"))
		def change = changeLists.first().changes.find{ it.beforeRevision.file.name.contains("RuleFieldValidator") }

		assert change.type == Change.Type.MOVED
		assert change.beforeRevision.file.name == "RuleFieldValidator.java"
		assert change.afterRevision.file.name == "RuleFieldValidator.java"
	}

	@Test "should ignore merge commits and include merge changes as separate change lists"() {
		def changeLists = requestChangeListsFor(jUnitProject, dateTime("10:00 09/05/2013"), dateTime("17:02 09/05/2013"))
		assert changeLists.size() == 1 : "merge commit 1ef54a1 was not ignored"

		def changes = changeLists.first().changes
		assert changes.first().beforeRevision.file.name == "ComparisonFailureTest.java"
	}

	static Project findJUnitProject() {
		def jUnitProject = ProjectManager.instance.openProjects.find{ it.name == "junit" }
		assert jUnitProject != null: "Couldn't find open project for junit framework"

		def sourceRoots = ProjectRootManager.getInstance(jUnitProject).contentSourceRoots.toList()
		def vcsRoot = ProjectLevelVcsManager.getInstance(jUnitProject).getVcsRootObjectFor(sourceRoots.first())
		assert vcsRoot.vcs.class.simpleName == "GitVcs"

		jUnitProject
	}
}
