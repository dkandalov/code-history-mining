package history
import com.intellij.openapi.project.Project
import history.events.ChangeEvent
import history.events.CommitInfo
import history.events.FileChangeInfo
import org.junit.Test

import static history.SourceOfChangeListsGitTest.findJUnitProject
import static history.events.ElementChangeInfo.getEMPTY
import static history.events.FileChangeInfo.getNA
import static history.util.DateTimeUtil.dateTime
import static history.util.DateTimeUtil.exactDateTime

class SourceOfChangeEventsGitTest {
	private final Project jUnitProject = findJUnitProject()

	@Test "loading events on file level"() {
		// setup
		def sourceOfChangeLists = new SourceOfChangeLists(jUnitProject, 1)
		def eventsExtractor = new ChangeEventsExtractor(jUnitProject)
		def eventsSource = new SourceOfChangeEvents(sourceOfChangeLists, eventsExtractor.&fileChangeEventsFrom)

		def commitComment = "Rename TestMethod -> JUnit4MethodRunner Rename methods in JUnit4MethodRunner to make run order clear"
		def commitInfo = new CommitInfo("43b0fe352d5bced0c341640d0c630d23f2022a7e", "dsaff <dsaff>", exactDateTime("15:42:16 03/10/2007"), commitComment)
		def expectedChangeEvents = [
				new ChangeEvent(commitInfo, new FileChangeInfo("Theories.java", "MODIFICATION", "/src/org/junit/experimental/theories", "", NA, NA), EMPTY),
				new ChangeEvent(commitInfo, new FileChangeInfo("TheoryMethodRunner.java", "MOVED", "/src/org/junit/experimental/theories/internal", "", NA, NA), EMPTY),
				new ChangeEvent(commitInfo, new FileChangeInfo("JUnit4ClassRunner.java", "MODIFICATION", "/src/org/junit/internal/runners", "", NA, NA), EMPTY),
				new ChangeEvent(commitInfo, new FileChangeInfo("JUnit4MethodRunner.java", "NEW", "", "/src/org/junit/internal/runners", NA, NA), EMPTY),
				new ChangeEvent(commitInfo, new FileChangeInfo("TestMethod.java", "MODIFICATION", "/src/org/junit/internal/runners", "", NA, NA), EMPTY),
				new ChangeEvent(commitInfo, new FileChangeInfo("StubbedTheories.java", "MODIFICATION", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "", NA, NA), EMPTY),
				new ChangeEvent(commitInfo, new FileChangeInfo("StubbedTheoryMethod.java", "MODIFICATION", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "", NA, NA), EMPTY),
				new ChangeEvent(commitInfo, new FileChangeInfo("TestMethodInterfaceTest.java", "MODIFICATION", "/src/org/junit/tests/extension", "", NA, NA), EMPTY)
		]

		// exercise
		eventsSource.request(dateTime("15:40 03/10/2007"), dateTime("15:45 03/10/2007")) { List changeEvents ->
			// verify
			assert expectedChangeEvents.size() == changeEvents.size()
			changeEvents.eachWithIndex { event, i ->
				def expectedEvent = expectedChangeEvents[i]
				assert event == expectedEvent
			}
		}
	}
}
