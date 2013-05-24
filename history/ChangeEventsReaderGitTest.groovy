package history
import com.intellij.openapi.project.Project
import history.events.*
import history.unused.CommitMethodsMunger
import history.unused.MethodChangeEvent
import org.junit.Test

import static CommitReaderGitTest.findJUnitProject
import static history.events.FileChangeInfo.getNA
import static history.util.DateTimeUtil.dateTime
import static history.util.DateTimeUtil.exactDateTime

class ChangeEventsReaderGitTest {
	private final Project jUnitProject = findJUnitProject()

	@Test "should read events on file level for one commit"() {
		// setup
		def commitReader = new CommitReader(jUnitProject, 1)
		def eventsReader = new ChangeEventsReader(commitReader, new CommitFilesMunger(jUnitProject, false).&mungeCommit)

		def commitComment = "Rename TestMethod -> JUnit4MethodRunner Rename methods in JUnit4MethodRunner to make run order clear"
		def commitInfo = new CommitInfo("43b0fe352d5bced0c341640d0c630d23f2022a7e", "dsaff <dsaff>", exactDateTime("15:42:16 03/10/2007"), commitComment)
		def expectedChangeEvents = [
				new FileChangeEvent(commitInfo, new FileChangeInfo("Theories.java", "MODIFICATION", "/src/org/junit/experimental/theories", "", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("TheoryMethodRunner.java", "MOVED", "/src/org/junit/experimental/theories/internal", "", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("JUnit4ClassRunner.java", "MODIFICATION", "/src/org/junit/internal/runners", "", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("JUnit4MethodRunner.java", "NEW", "", "/src/org/junit/internal/runners", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("TestMethod.java", "MODIFICATION", "/src/org/junit/internal/runners", "", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("StubbedTheories.java", "MODIFICATION", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("StubbedTheoryMethod.java", "MODIFICATION", "/src/org/junit/tests/experimental/theories/extendingwithstubs", "", NA, NA)),
				new FileChangeEvent(commitInfo, new FileChangeInfo("TestMethodInterfaceTest.java", "MODIFICATION", "/src/org/junit/tests/extension", "", NA, NA))
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

	@Test "should read events on method level for one commit"() {
		// setup
		def commitReader = new CommitReader(jUnitProject, 1)
		def eventsReader = new ChangeEventsReader(commitReader, new CommitMethodsMunger(jUnitProject).&mungeCommit)

		def commitComment = "Added support for iterable datapoints"
		def commitInfo = new CommitInfo("b421d0ebd66701187c10c2b0c7f519dc435531ae", "Tim Perry", exactDateTime("19:37:57 01/04/2013"), commitComment)
		def changedFile1 = new FileChangeInfo("AllMembersSupplier.java", "MODIFICATION", "/src/main/java/org/junit/experimental/theories/internal", "", 178, 204)
		def changedFile2 = new FileChangeInfo("AllMembersSupplierTest.java", "MODIFICATION", "/src/test/java/org/junit/tests/experimental/theories/internal", "", 156, 209)
		def byFileAndElement = { it.fileName + it.elementName }
		def expectedChangeEvents = [
				new MethodChangeEvent(commitInfo, changedFile1, new ElementChangeInfo("AllMembersSupplier::addSinglePointFields", 9, 9, 380, 380)),
				new MethodChangeEvent(commitInfo, changedFile1, new ElementChangeInfo("AllMembersSupplier", 160, 185, 6121, 7143)),
				new MethodChangeEvent(commitInfo, changedFile1, new ElementChangeInfo("AllMembersSupplier::addArrayValues", 8, 8, 379, 379)),
				new MethodChangeEvent(commitInfo, changedFile1, new ElementChangeInfo("AllMembersSupplier::addMultiPointMethods", 18, 20, 934, 1049)),
				new MethodChangeEvent(commitInfo, changedFile1, new ElementChangeInfo("", 0, 0, 0, 0)),
				new MethodChangeEvent(commitInfo, changedFile1, new ElementChangeInfo("AllMembersSupplier::addIterableValues", 0, 11, 0, 455)),
				new MethodChangeEvent(commitInfo, changedFile1, new ElementChangeInfo("AllMembersSupplier::addDataPointsValues", 0, 9, 0, 380)),

				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::allMemberValuesFor", 7, 7, 387, 387)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::dataPointsCollectionFieldsShouldBeRecognized", 0, 7, 0, 286)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::dataPointsCollectionShouldBeRecognizedIgnoringStrangeTypes", 0, 7, 0, 322)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::dataPointsCollectionMethodShouldBeRecognized", 0, 7, 0, 287)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::HasDataPointsListField", 0, 8, 0, 211)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::HasDataPointsListMethod", 0, 10, 0, 246)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("", 0, 0, 0, 0)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::HasDataPointsListFieldWithOverlyGenericTypes", 0, 8, 0, 243)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest", 135, 187, 4466, 6104)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::HasDataPointsListField::theory", 3, 3, 58, 60)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest", 135, 187, 4466, 6104)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::HasDataPointsListFieldWithOverlyGenericTypes::theory", 3, 3, 58, 60)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::HasDataPointsListMethod::getList", 0, 4, 0, 116)),
				new MethodChangeEvent(commitInfo, changedFile2, new ElementChangeInfo("AllMembersSupplierTest::HasDataPointsListMethod::theory", 3, 3, 58, 60))
		].sort(byFileAndElement)

		// exercise
		eventsReader.readPresentToPast(dateTime("19:30 01/04/2013"), dateTime("19:40 01/04/2013")) { List changeEvents ->
			// verify
			assert expectedChangeEvents.size() == changeEvents.size()

			changeEvents.sort(byFileAndElement).eachWithIndex { event, i ->
				def expectedEvent = expectedChangeEvents[i]
				assert event == expectedEvent
			}
		}
	}
}
