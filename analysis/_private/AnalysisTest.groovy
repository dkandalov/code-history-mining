package analysis._private
import events.CommitInfo
import events.FileChangeEvent
import events.FileChangeInfo
import org.junit.Test

import static events.ChangeStats.NA
import static util.DateTimeUtil.date

class AnalysisTest {

	@Test void "graph with all files and all committers"() {
		def changeEvents = [
			commitBy(TimPerry,  "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(DavidSaff, "02/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(KentBeck,  "02/04/2013", modified("/theories/Theories.java"))
		].flatten()

		assert Analysis.commitLogAsGraph(changeEvents, {}, 100) == """
      |"nodes": [{"name": "/theories/internal/AllMembersSupplier.java", "group": 1},
      |{"name": "/theories/internal/AllMembersSupplier.java", "group": 1},
      |{"name": "/theories/Theories.java", "group": 1},
      |{"name": "Tim Perry", "group": 2},
      |{"name": "David Saff", "group": 2},
      |{"name": "Kent Beck", "group": 2}],
      |"links": [{"source": 3, "target": 0, "value": 1},
      |{"source": 4, "target": 0, "value": 1},
      |{"source": 5, "target": 2, "value": 1}]
		""".stripMargin("|").trim()
	}

	@Test void "graph with authors changing same files within a week"() {
		def changeEvents = [
			commitBy(TimPerry,  "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(DavidSaff, "02/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(KentBeck,  "02/04/2013", modified("/theories/Theories.java")),
			commitBy(TimPerry,  "01/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(DavidSaff, "01/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
		].flatten()

		def threshold = 2
		assert Analysis.authorChangingSameFilesGraph(changeEvents, {}, threshold) == """
      |"nodes": [{"name": "/theories/internal/AllMembersSupplier.java", "group": 1},
      |{"name": "Tim Perry", "group": 2},
      |{"name": "David Saff", "group": 2}],
      |"links": [{"source": 1, "target": 0, "value": 2},
      |{"source": 2, "target": 0, "value": 2}]
		""".stripMargin("|").trim()
	}

	@Test void "graph with files changed in the same commit"() {
		def changeEvents = [
			commitBy(TimPerry,  "03/04/2013",
				modified("/theories/internal/AllMembersSupplier.java"),
				modified("/theories/Theories.java")
			),
			commitBy(TimPerry,  "01/04/2013",
				modified("/theories/internal/AllMembersSupplier.java"),
				modified("/theories/Theories.java"),
				modified("/textui/ResultPrinter.java")
			)
		].flatten()

		def threshold = 2
		assert Analysis.filesInTheSameCommitGraph(changeEvents, {}, threshold) == """
      |"nodes": [{"name": "/theories/Theories.java", "group": 1},
      |{"name": "/theories/internal/AllMembersSupplier.java", "group": 1}],
      |"links": [{"source": 0, "target": 1, "value": 2}]
		""".stripMargin("|").trim()
	}

	private List<FileChangeEvent> commitBy(String author, String dateAsString, FileChangeInfo... fileChanges) {
		def commitInfo = new CommitInfo(someRevision(), author, date(dateAsString), someCommitMessage)
		fileChanges.collect { new FileChangeEvent(commitInfo, it) }
	}

	private static FileChangeInfo modified(String filePath) {
		def i = filePath.lastIndexOf("/")
		def path = filePath.substring(0, i)
		def file = filePath.substring(i + 1)
		new FileChangeInfo("", file, "", path, "MODIFICATION", NA, NA)
	}

	private static final String TimPerry = "Tim Perry"
	private static final String DavidSaff = "David Saff"
	private static final String KentBeck = "Kent Beck"

	private int revision = 1
	private final Closure<String> someRevision = { (revision++).toString() }
	private final someCommitMessage = ""
}
