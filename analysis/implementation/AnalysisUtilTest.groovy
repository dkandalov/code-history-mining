package analysis.implementation
import common.events.CommitInfo
import common.events.FileChangeEvent
import common.events.FileChangeInfo
import org.junit.Test

import static analysis.implementation.Analysis.Util.useLatestNameForMovedFiles
import static common.events.ChangeStats.getNA

class AnalysisUtilTest {
	private final fileEvents = new FileEvents()

	@Test void "changing renamed file name to its latest name"() {
	  def events = fileEvents.with{[
			  modified("/theories/Theories2.java"),
			  moved("/theories/Theories.java", "/theories/Theories2.java"),
			  modified("/theories/Theories.java"),
			  created("/theories/Theories.java"),
	  ]}
		def expectedEvents = fileEvents.with{[
				modified("/theories/Theories2.java"),
				moved("/theories/Theories.java", "/theories/Theories2.java"),
				modified("/theories/Theories2.java"),
				created("/theories/Theories2.java"),
		]}
		assert useLatestNameForMovedFiles(events) == expectedEvents
	}

	@Test void "changing twice renamed file name to its latest name"() {
	  def events = fileEvents.with{[
			  modified("/theories/Theories3.java"),
			  moved("/theories/Theories2.java", "/theories/Theories3.java"),
			  moved("/theories/Theories.java", "/theories/Theories2.java"),
			  modified("/theories/Theories.java"),
			  created("/theories/Theories.java"),
	  ]}
		def expectedEvents = fileEvents.with{[
				modified("/theories/Theories3.java"),
				moved("/theories/Theories2.java", "/theories/Theories3.java", "MOVED"),
				moved("", "/theories/Theories3.java", "MOVED_UNDONE"),
				modified("/theories/Theories3.java"),
				created("/theories/Theories3.java"),
		]}
		assert useLatestNameForMovedFiles(events) == expectedEvents
	}

	static class FileEvents {
		private final Date someDate = new Date(0)
		private final String someRevision = "1"
		private final String someAuthor = ""
		private final String someCommitMessage = ""
		private final CommitInfo someCommitInfo = new CommitInfo(someRevision, someAuthor, someDate, someCommitMessage)

		private FileChangeEvent created(String fullFileName) {
			def (packageName, fileName) = splitByLast("/", fullFileName)
			new FileChangeEvent(someCommitInfo, new FileChangeInfo("", fileName, "", packageName, "NEW", NA, NA))
		}

		private FileChangeEvent modified(String fullFileName) {
			def (packageName, fileName) = splitByLast("/", fullFileName)
			new FileChangeEvent(someCommitInfo, new FileChangeInfo("", fileName, "", packageName, "MODIFICATION", NA, NA))
		}

		private FileChangeEvent moved(String from, String to, String modificationType = "MOVED") {
			def (fromPackage, fromFile) = splitByLast("/", from)
			def (toPackage, toFile) = splitByLast("/", to)
			new FileChangeEvent(someCommitInfo, new FileChangeInfo(fromFile, toFile, fromPackage, toPackage, modificationType, NA, NA))
		}

		private static splitByLast(String symbol, String s) {
			if (s.empty) return ["", ""]
			def i = s.lastIndexOf(symbol)
			[s.substring(0, i), s.substring(i + 1)]
		}
	}
}
