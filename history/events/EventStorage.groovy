package history.events
import com.intellij.openapi.util.io.FileUtil

import java.text.SimpleDateFormat

class EventStorage {
	private static final String CSV_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z"

	final String filePath

	EventStorage(String filePath) {
		this.filePath = filePath
	}

	List<FileChangeEvent> readAllEvents(whenFiledToParseLine = {}) {
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

	def appendToEventsFile(Collection<FileChangeEvent> changeEvents) {
		if (changeEvents.empty) return
		appendTo(filePath, toCsv(changeEvents))
	}

	def prependToEventsFile(Collection<FileChangeEvent> changeEvents) {
		if (changeEvents.empty) return
		prependTo(filePath, toCsv(changeEvents))
	}

	Date getOldestEventTime() {
		def line = readLastLine(filePath)
		line == null ? null :fromCsv(line).revisionDate
	}

	Date getMostRecentEventTime() {
		def line = readFirstLine(filePath)
		line == null ? null : fromCsv(line).revisionDate
	}

	boolean hasNoEvents() {
		def file = new File(filePath)
		!file.exists() || file.length() == 0
	}

	private static String toCsv(Collection<FileChangeEvent> changeEvents) {
		changeEvents.collect{toCsv(it)}.join("\n") + "\n"
	}

	private static String toCsv(FileChangeEvent changeEvent) { // TODO use proper csv (e.g. files can have commas in name)
		changeEvent.with {
			def commitMessageEscaped = '"' + commitMessage.replaceAll("\"", "\\\"").replaceAll("\n", "\\\\n") + '"'
			[format(revisionDate), revision, author, fileName, fileChangeType, packageBefore, packageAfter,
					lines.before, lines.after, lines.added, lines.modified, lines.removed,
					chars.before, chars.after, chars.added, chars.modified, chars.removed,
					commitMessageEscaped].join(",")
		}
	}

	private static FileChangeEvent fromCsv(String line) {
		def (revisionDate, revision, author, fileName, fileChangeType, packageBefore, packageAfter,
				linesBefore, linesAfter, linesAdded, linesModified, linesRemoved,
				charsBefore, charsAfter, charsAdded, charsModified, charsRemoved
		) = line.split(",")
		def commitMessage = line.substring(line.indexOf('"') + 1, line.size() - 1)
		revisionDate = new SimpleDateFormat(CSV_DATE_FORMAT).parse(revisionDate)

		def event = new FileChangeEvent(
				new CommitInfo(revision, author, revisionDate, commitMessage),
				new FileChangeInfo(fileName, fileChangeType, packageBefore, packageAfter,
						new ChangeStats(asInt(linesBefore), asInt(linesAfter), asInt(linesAdded), asInt(linesModified), asInt(linesRemoved)),
						new ChangeStats(asInt(charsBefore), asInt(charsAfter), asInt(charsAdded), asInt(charsModified), asInt(charsRemoved))
				)
		)
		event
	}

	private static int asInt(String s) { s.toInteger() }

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
		def tempFile = FileUtil.createTempFile("code_history_mining", "_${new Random().nextInt(10000)}")
		def file = new File(filePath)

		tempFile.withOutputStream { output ->
			output.write(text.bytes)
			if (!file.exists()) return

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
