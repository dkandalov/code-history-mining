package history.events
import com.intellij.openapi.util.io.FileUtil

import java.text.SimpleDateFormat

class EventStorage {
	static final String CSV_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z"

	final String filePath

	EventStorage(String filePath) {
		this.filePath = filePath
	}

	List<ChangeEvent> readAllEvents(whenFiledToParseLine = {}) {
		def events = []
		new File(filePath).withReader { reader ->
			def line
			while ((line = reader.readLine()) != null) {
				try {
					events << fromCsv(line)
				} catch (Exception e) {
					whenFiledToParseLine(line, e)
				}
			}
		}
		events
	}

	def appendToEventsFile(Collection<ChangeEvent> changeEvents) {
		if (changeEvents.empty) return
		appendTo(filePath, toCsv(changeEvents))
	}

	def prependToEventsFile(Collection<ChangeEvent> changeEvents) {
		if (changeEvents.empty) return
		prependTo(filePath, toCsv(changeEvents))
	}

	Date getOldestEventTime() {
		def line = readLastLine(filePath)
		if (line == null) null
		else {
			// minus one second because git "before" seems to be inclusive (even though ChangeBrowserSettings API is exclusive)
			// (it means that if processing stops between two commits that happened on the same second,
			// we will miss one of them.. considered this to be insignificant)
			def date = fromCsv(line).revisionDate
			date.time -= 1000
			date
		}
	}

	Date getMostRecentEventTime() {
		def line = readFirstLine(filePath)
		if (line == null) new Date()
		else {
			def date = fromCsv(line).revisionDate
			date.time += 1000 // plus one second (see comments in getOldestEventTime())
			date
		}
	}

	boolean hasNoEvents() {
		def file = new File(filePath)
		!file.exists() || file.length() == 0
	}

	private static String toCsv(Collection<ChangeEvent> changeEvents) {
		changeEvents.collect{toCsv(it)}.join("\n") + "\n"
	}

	private static String toCsv(ChangeEvent changeEvent) {
		changeEvent.with {
			def commitMessageEscaped = '"' + commitMessage.replaceAll("\"", "\\\"").replaceAll("\n", "\\\\n") + '"'
			[format(revisionDate), revision, author, fileName, fileChangeType,
					packageBefore, packageAfter, linesInFileBefore, linesInFileAfter, commitMessageEscaped].join(",")
		}
	}

	private static ChangeEvent fromCsv(String line) {
		def (revisionDate, revision, author, fileName, fileChangeType,
				packageBefore, packageAfter, linesInFileBefore, linesInFileAfter) = line.split(",")
		def commitMessage = line.substring(line.indexOf('"') + 1, line.size() - 1)
		revisionDate = new SimpleDateFormat(CSV_DATE_FORMAT).parse(revisionDate)

		def event = new ChangeEvent(
				new CommitInfo(revision, author, revisionDate, commitMessage),
				new FileChangeInfo(fileName, fileChangeType, packageBefore, packageAfter, linesInFileBefore.toInteger(), linesInFileAfter.toInteger()),
		)
		event
	}

	private static String format(Date date) {
		new SimpleDateFormat(CSV_DATE_FORMAT).format(date)
	}

	private static String readFirstLine(String filePath) {
		def file = new File(filePath)
		if (!file.exists() || file.length() == 0) return null
		file.withReader{ it.readLine() }
	}

	private static String readLastLine(String filePath) {
		def file = new File(filePath)
		if (!file.exists() || file.length() == 0) return null

		def randomAccess = new RandomAccessFile(file, "r")
		try {

			int shift = 1 // shift in case file ends with single newline
			for (long pos = file.length() - 1 - shift; pos >= 0; pos--) {
				randomAccess.seek(pos)
				if (randomAccess.read() == '\n') {
					return randomAccess.readLine()
				}
			}
			// assume that file has only one line
			randomAccess.seek(0)
			randomAccess.readLine()

		} finally {
			randomAccess.close()
		}
	}

	private static appendTo(String filePath, String text) {
		def file = new File(filePath)
		FileUtil.createParentDirs(file)
		file.append(text)
	}

	private static prependTo(String filePath, String text) {
		def tempFile = FileUtil.createTempFile("delta_flora", "_${new Random().nextInt(10000)}")
		def file = new File(filePath)

		tempFile.withOutputStream { output ->
			output.write(text.bytes)
			file.withInputStream { input ->
				// magic buffer size is copied from com.intellij.openapi.util.io.FileUtilRt#BUFFER (assume there is a reason for it)
				byte[] buffer = new byte[1024 * 20]
				while (true) {
					int read = input.read(buffer)
					if (read < 0) break
					output.write(buffer, 0, read)
				}
			}
		}

		file.delete()
		FileUtil.rename(tempFile, file)
	}
}
