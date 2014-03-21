package analysis._private
import events.CommitInfo
import events.FileChangeEvent
import events.FileChangeInfo
import org.junit.Test

import static events.ChangeStats.NA
import static util.DateTimeUtil.date
import static util.DateTimeUtil.oneDay

class AnalysisTest {
	private static final Closure noCancel = {}
	private final commitEvents = new CommitEvents()

	@Test void "amount of changing files adds data for days without changes"() {
		def changeEvents = commitEvents.with{[
				commitBy(someone, "05/04/2013", modified("/theories/internal/Theories.java")),
				commitBy(someone, "03/04/2013", modified("/theories/internal/Assignments.java")),
		].flatten()}

		assert Analysis.amountOfChangingFiles_Chart(changeEvents, noCancel, [oneDay]) == """
			|["\\
	    |date,category,value\\n\\
      |03/04/2013,unchanged,0\\n\\
      |04/04/2013,unchanged,1\\n\\
      |05/04/2013,unchanged,1\\n\\
	    |03/04/2013,recently changed,1\\n\\
      |04/04/2013,recently changed,0\\n\\
      |05/04/2013,recently changed,1\\n\\
			|"]
		""".stripMargin("|").trim()
	}

	@Test void "amount of changing files takes into account deleted files"() {
		def changeEvents = commitEvents.with{[
				commitBy(someone, "04/04/2013", deleted("/theories/internal/Assignments.java")),
				commitBy(someone, "03/04/2013", created("/theories/internal/Assignments.java")),
		].flatten()}

		assert Analysis.amountOfChangingFiles_Chart(changeEvents, noCancel, [oneDay]) == """
			|["\\
	    |date,category,value\\n\\
      |03/04/2013,unchanged,0\\n\\
      |04/04/2013,unchanged,0\\n\\
	    |03/04/2013,recently changed,1\\n\\
      |04/04/2013,recently changed,0\\n\\
			|"]
		""".stripMargin("|").trim()
	}

	@Test void "amount of changing files when changing files on different days"() {
		def changeEvents = commitEvents.with{[
				commitBy(someone, "04/04/2013", modified("/theories/internal/Assignments.java")),
				commitBy(someone, "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
		].flatten()}

		assert Analysis.amountOfChangingFiles_Chart(changeEvents, noCancel, [oneDay]) == """
			|["\\
	    |date,category,value\\n\\
      |03/04/2013,unchanged,0\\n\\
      |04/04/2013,unchanged,1\\n\\
	    |03/04/2013,recently changed,1\\n\\
      |04/04/2013,recently changed,1\\n\\
			|"]
		""".stripMargin("|").trim()
	}

	@Test void "amount of changing files when modified single file"() {
		def changeEvents = commitEvents.with{[
				commitBy(someone, "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
		].flatten()}

		assert Analysis.amountOfChangingFiles_Chart(changeEvents, noCancel, [oneDay]) == """
			|["\\
			|date,category,value\\n\\
      |03/04/2013,unchanged,0\\n\\
      |03/04/2013,recently changed,1\\n\\
			|"]
		""".stripMargin("|").trim()
	}

	@Test void "change size by file type puts least changed file types into 'others' category"() {
		def changeEvents = commitEvents.with{(
				(0..100).collect{ commitBy(someone, "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")) } +
				(0..100).collect{ commitBy(someone, "03/04/2013", modified("/pom.xml")) } +
				(0..1).collect{ commitBy(someone, "02/04/2013", modified("/acknowledgements.txt")) } +
				(0..1).collect{ commitBy(someone, "02/04/2013", modified("/logo.gif")) }
		).flatten()}

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
		def changeEvents = commitEvents.with{[
				commitBy(someone, "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
				commitBy(someone, "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
				commitBy(someone, "02/04/2013", modified("/theories/internal/Assignments.java")),
				commitBy(someone, "02/04/2013", modified("/logo.gif"))
		].flatten()}

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
		def changeEvents = commitEvents.with{[
			commitBy(TimPerry,  "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(DavidSaff, "02/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(KentBeck,  "02/04/2013", modified("/theories/Theories.java"))
		].flatten()}

		assert Analysis.commitLog_Graph(changeEvents, noCancel, 100) == """
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
		def changeEvents = commitEvents.with{[
			commitBy(TimPerry,  "03/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(DavidSaff, "02/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(KentBeck,  "02/04/2013", modified("/theories/Theories.java")),
			commitBy(TimPerry,  "01/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
			commitBy(DavidSaff, "01/04/2013", modified("/theories/internal/AllMembersSupplier.java")),
		].flatten()}

		def threshold = 2
		assert Analysis.authorChangingSameFiles_Graph(changeEvents, noCancel, threshold) == """
      |"nodes": [{"name": "/theories/internal/AllMembersSupplier.java", "group": 1},
      |{"name": "Tim Perry", "group": 2},
      |{"name": "David Saff", "group": 2}],
      |"links": [{"source": 1, "target": 0, "value": 2},
      |{"source": 2, "target": 0, "value": 2}]
		""".stripMargin("|").trim()
	}

	@Test void "graph with files changed in the same commit"() {
		def changeEvents = commitEvents.with{[
			commitBy(TimPerry,  "03/04/2013",
				modified("/theories/internal/AllMembersSupplier.java"),
				modified("/theories/Theories.java")
			),
			commitBy(TimPerry,  "01/04/2013",
				modified("/theories/internal/AllMembersSupplier.java"),
				modified("/theories/Theories.java"),
				modified("/textui/ResultPrinter.java")
			)
		].flatten()}

		def threshold = 2
		assert Analysis.filesInTheSameCommit_Graph(changeEvents, noCancel, threshold) == """
      |"nodes": [{"name": "/theories/Theories.java", "group": 1},
      |{"name": "/theories/internal/AllMembersSupplier.java", "group": 1}],
      |"links": [{"source": 0, "target": 1, "value": 2}]
		""".stripMargin("|").trim()
	}

	static class CommitEvents {
		List<FileChangeEvent> commitBy(String author, String dateAsString, FileChangeInfo... fileChanges) {
			def commitInfo = new CommitInfo(nextRevision(), author, date(dateAsString), someCommitMessage)
			fileChanges.collect { new FileChangeEvent(commitInfo, it) }
		}

		static FileChangeInfo modified(String filePath) {
			def (path, file) = splitByLast("/", filePath)
			new FileChangeInfo("", file, "", path, "MODIFICATION", NA, NA)
		}

		static FileChangeInfo created(String filePath) {
			def (path, file) = splitByLast("/", filePath)
			new FileChangeInfo("", file, "", path, "NEW", NA, NA)
		}

		static FileChangeInfo deleted(String filePath) {
			def (path, file) = splitByLast("/", filePath)
			new FileChangeInfo(file, "", "", path, "DELETED", NA, NA)
		}

		private static splitByLast(String symbol, String s) {
			if (s.empty) return ["", ""]
			def i = s.lastIndexOf(symbol)
			[s.substring(0, i), s.substring(i + 1)]
		}

		static final String someone = ""
		static final String TimPerry = "Tim Perry"
		static final String DavidSaff = "David Saff"
		static final String KentBeck = "Kent Beck"

		private int revision = 1
		private final Closure<String> nextRevision = { (revision++).toString() }

		private final String someCommitMessage = ""
	}
}
