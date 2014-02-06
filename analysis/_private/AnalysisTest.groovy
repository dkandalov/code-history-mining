package analysis._private
import events.CommitInfo
import events.FileChangeEvent
import events.FileChangeInfo
import org.junit.Test

import static events.ChangeStats.NA
import static util.DateTimeUtil.date

class AnalysisTest {

	@Test void "creates json for latest commits as graph"() {
		def changeEvents = [
			commitBy(TimPerry, "03/04/2013"){ modified("/theories/internal/AllMembersSupplier.java") },
			commitBy(DavidSaff, "02/04/2013"){ modified("/theories/internal/AllMembersSupplier.java") }
		].flatten()

		def now = date("04/04/2013")
		assert Analysis.commitLogAsGraph(changeEvents, {}, 100, now) == """
			|"nodes": [{"name": "/theories/internal/AllMembersSupplier.java", "group": 1},
			|{"name": "/theories/internal/AllMembersSupplier.java", "group": 1},
			|{"name": "Tim Perry", "group": 2},
			|{"name": "David Saff", "group": 2}],
			|"links": [{"source": 2, "target": 0, "value": 1},
			|{"source": 3, "target": 0, "value": 1}]
		""".stripMargin("|").trim()
	}

	private List<FileChangeEvent> commitBy(String author, String dateAsString, Closure createFileChangeInfos) {
		def commitInfo = new CommitInfo(someRevision(), author, date(dateAsString), someCommitMessage)
		createFileChangeInfos().collect {
			new FileChangeEvent(commitInfo, it)
		}
	}

	private static FileChangeInfo modified(String filePath) {
		def i = filePath.lastIndexOf("/")
		def path = filePath.substring(0, i)
		def file = filePath.substring(i + 1)
		new FileChangeInfo("", file, "", path, "MODIFICATION", NA, NA)
	}

	private static final String TimPerry = "Tim Perry"
	private static final String DavidSaff = "David Saff"

	private int revision = 1
	private final Closure<String> someRevision = { (revision++).toString() }
	private final someCommitMessage = ""
}
