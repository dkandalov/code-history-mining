package history.events
import org.junit.After
import org.junit.Before
import org.junit.Test

import static history.util.DateTimeUtil.exactDateTime
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class EventStorageTest {

	@Test void "should append events to a file"() {
		storage.appendToEventsFile([event1])
		assertThat(new File(temporaryFile).readLines().join("\n"), equalTo(event1AsCsv))

		storage.appendToEventsFile([event2])
		assertThat(new File(temporaryFile).readLines().join("\n"), equalTo(
				event1AsCsv + "\n" +
				event2AsCsv
		))
	}

	@Test void "should prepend events to a file"() {
		storage.prependToEventsFile([event1, event2])
		assertThat(new File(temporaryFile).readLines().join("\n"), equalTo(
				event1AsCsv + "\n" +
				event2AsCsv
		))

		storage.prependToEventsFile([event1])
		assertThat(new File(temporaryFile).readLines().join("\n"), equalTo(
				event1AsCsv + "\n" +
				event1AsCsv + "\n" +
				event2AsCsv
		))
	}

	@Test void "should read all events from file"() {
		new File(temporaryFile).write(event1AsCsv + "\n" + event2AsCsv)
		storage.readAllEvents().with{
			assert it[0] == event1
			assert it[1] == event2
		}
	}

	@Test void "should read time of most recent and oldest events"() {
		new File(temporaryFile).write(event1AsCsv + "\n" + event2AsCsv)

		assert storage.mostRecentEventTime == event1.revisionDate
		assert storage.oldestEventTime == event2.revisionDate
	}

	@Before void setUp() {
		temporaryFile = new File("test-events-file${new Random().nextInt(10000)}.csv").absolutePath
		storage = new EventStorage(temporaryFile)
	}

	@After void tearDown() {
		if (new File(temporaryFile).exists()) {
			new File(temporaryFile).delete()
		}
	}

	private String temporaryFile
	private final event1 = new FileChangeEvent(
			new CommitInfo("b421d0ebd66701187c10c2b0c7f519dc435531ae", "Tim Perry", exactDateTime("19:37:57 01/04/2013"), "Added support for iterable datapoints"),
			new FileChangeInfo("AllMembersSupplier.java", "/src/main/java/org/junit/experimental/theories/internal", "", "MODIFICATION",
					new ChangeStats(178, 204, 23, 3, 0), new ChangeStats(6758, 7807, 878, 304, 0)
			)
	)
	private final event2 = new FileChangeEvent(
			new CommitInfo("43b0fe352d5bced0c341640d0c630d23f2022a7e", "dsaff <dsaff>", exactDateTime("15:42:16 03/10/2007"), "Rename TestMethod -> JUnit4MethodRunner"),
			new FileChangeInfo("Theories.java", "/src/org/junit/experimental/theories", "", "MODIFICATION",
					new ChangeStats(37, 37, 0, 4, 0), new ChangeStats(950, 978, 0, 215, 0)
			)
	)
	private final event1AsCsv = "2013-04-01 19:37:57 +0100,b421d0ebd66701187c10c2b0c7f519dc435531ae,Tim Perry,AllMembersSupplier.java,/src/main/java/org/junit/experimental/theories/internal,,MODIFICATION,178,204,23,3,0,6758,7807,878,304,0,\"Added support for iterable datapoints\""
	private final event2AsCsv = "2007-10-03 15:42:16 +0100,43b0fe352d5bced0c341640d0c630d23f2022a7e,dsaff <dsaff>,Theories.java,/src/org/junit/experimental/theories,,MODIFICATION,37,37,0,4,0,950,978,0,215,0,\"Rename TestMethod -> JUnit4MethodRunner\""
	private EventStorage storage

}
