package analysis._private
import events.CommitInfo
import events.FileChangeEvent
import events.FileChangeInfo
import org.junit.Test

import static events.ChangeStats.NA
import static util.DateTimeUtil.date

class AnalysisTest {

	@Test void "change size by file type puts least changed file types into 'others' category"() {
		def changeEvents = (
				(0..100).collect{ commitBy(TimPerry,  "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")) } +
				(0..100).collect{ commitBy(DavidSaff, "03/04/2013", modified("/pom.xml")) } +
				(0..1).collect{ commitBy(KentBeck,  "02/04/2013", modified("/acknowledgements.txt")) } +
				(0..1).collect{ commitBy(KentBeck,  "02/04/2013", modified("/logo.gif")) }
		).flatten()

		def maxAmountOfFileTypes = 2
		assert Analysis.changeSizeByFileType_Chart(changeEvents, noCancel, maxAmountOfFileTypes).startsWith("""
			|["\\
			|date,category,value\\n\\
      |03/04/2013,java,101\\n\\
      |02/04/2013,java,0\\n\\
      |03/04/2013,xml,101\\n\\
      |02/04/2013,xml,0\\n\\
      |03/04/2013,Other,0\\n\\
      |02/04/2013,Other,4\\n\\
			|",
		""".stripMargin("|").trim())
	}

	@Test void "change size by file type chart"() {
		def changeEvents = [
				commitBy(TimPerry,  "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
				commitBy(DavidSaff, "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
				commitBy(DavidSaff, "02/04/2013", modified("/theories/internal/Assignments.java")),
				commitBy(KentBeck,  "02/04/2013", modified("/logo.gif"))
		].flatten()

		assert Analysis.changeSizeByFileType_Chart(changeEvents).startsWith("""
			|["\\
      |date,category,value\\n\\
      |03/04/2013,java,2\\n\\
      |02/04/2013,java,1\\n\\
      |03/04/2013,gif,0\\n\\
      |02/04/2013,gif,1\\n\\
      |",
		""".stripMargin("|").trim())
	}

	@Test void "graph with all files and all committers"() {
		def changeEvents = [
			commitBy(TimPerry,  "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(DavidSaff, "02/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(KentBeck,  "02/04/2013", modified("/theories/Theories.java"))
		].flatten()

		assert Analysis.commitLog_Graph(changeEvents, {}, 100) == """
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
		assert Analysis.authorChangingSameFiles_Graph(changeEvents, {}, threshold) == """
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
		assert Analysis.filesInTheSameCommit_Graph(changeEvents, {}, threshold) == """
      |"nodes": [{"name": "/theories/Theories.java", "group": 1},
      |{"name": "/theories/internal/AllMembersSupplier.java", "group": 1}],
      |"links": [{"source": 0, "target": 1, "value": 2}]
		""".stripMargin("|").trim()
	}

	private List<FileChangeEvent> commitBy(String author, String dateAsString, FileChangeInfo... fileChanges) {
		def commitInfo = new CommitInfo(nextRevision(), author, date(dateAsString), someCommitMessage)
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
	private final Closure<String> nextRevision = { (revision++).toString() }
	private final someCommitMessage = ""
	private static final Closure noCancel = {}
}
