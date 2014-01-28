package historyreader

import com.intellij.openapi.project.Project
import events.CommitInfo
import events.FileChangeEvent
import events.FileChangeInfo
import org.junit.Test

import static CommitReaderGitTest.findJUnitProject
import static events.FileChangeInfo.getNA
import static util.DateTimeUtil.dateTime
import static util.DateTimeUtil.exactDateTime

class ChangeEventsReaderGitTest {
	private final Project jUnitProject = findJUnitProject()

	@Test "should read events on file level for one commit"() {
		// setup
		def commitReader = new CommitReader(jUnitProject, 1)
		def eventsReader = new ChangeEventsReader(commitReader, new CommitFilesMunger(jUnitProject, false).&mungeCommit)

		def commitComment = "Rename TestMethod -> JUnit4MethodRunner Rename methods in JUnit4MethodRunner to make run order clear"
		def commitInfo = new CommitInfo("43b0fe352d5bced0c341640d0c630d23f2022a7e", "dsaff <dsaff>", exactDateTime("15:42:16 03/10/2007"), commitComment)
		def expectedChangeEvents = [
				new FileChangeEvent(commitInfo, new FileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", "MODIFICATION", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("TheoryMethod.java", "TheoryMethodRunner.java", "/src/org/junit/experimental/theories/internal", "/src/org/junit/experimental/theories/internal", "MOVED", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("", "JUnit4ClassRunner.java", "", "/src/org/junit/internal/runners", "MODIFICATION", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("", "JUnit4MethodRunner.java", "", "/src/org/junit/internal/runners", "NEW", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("", "TestMethod.java", "", "/src/org/junit/internal/runners", "MODIFICATION", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("", "StubbedTheories.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("", "StubbedTheoryMethod.java", "", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "MODIFICATION", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("", "TestMethodInterfaceTest.java", "", "/src/org/junit/tests/extension", "MODIFICATION", NA, NA))
		]

		// exercise
		eventsReader.readPresentToPast(dateTime("15:40 03/10/2007"), dateTime("15:45 03/10/2007")) { List changeEvents ->
			// verify
			assert expectedChangeEvents.size() == changeEvents.size()
			changeEvents.eachWithIndex { event, i ->
				def expectedEvent = expectedChangeEvents[i]
				assert event == expectedEvent
			}
		}
	}
}
