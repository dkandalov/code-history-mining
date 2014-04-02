package historystorage
import com.intellij.openapi.util.io.FileUtil
import common.events.ChangeStats
import common.events.CommitInfo
import common.events.FileChangeEvent
import common.events.FileChangeInfo
import groovy.transform.CompileStatic
import historystorage.csv4180.CSVReader
import historystorage.csv4180.CSVWriter

import java.text.SimpleDateFormat

class EventStorage {
	final String filePath
	private Date oldestEventTime
	private Date mostRecentEventTime

	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")


	EventStorage(String filePath = null, TimeZone timeZone = TimeZone.default) {
		this.filePath = filePath
		this.dateFormat.timeZone = timeZone
	}

	@CompileStatic
	List<FileChangeEvent> readAllEvents(Closure checkIfCancelled = {}, Closure whenFailedToParseLine = {line, e ->}) {
		def events = []
		def reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(filePath)))
		try {
			def line
			while ((line = reader.readLine()) != null) {
				try {
					events << fromCsv(line)
				} catch (Exception e) {
					whenFailedToParseLine(line, e)
				}
				checkIfCancelled()
			}
		} finally {
			reader.close()
		}
		events
	}

	boolean appendToEventsFile(Collection<FileChangeEvent> changeEvents) {
		def events = changeEvents.findAll{ getOldestEventTime() == null || it.revisionDate <= getOldestEventTime() }
		if (events.empty) return events.size() == changeEvents.size()

		appendTo(filePath, toCsv(events))
		oldestEventTime = events.last().revisionDate

		events.size() == changeEvents.size()
	}

	boolean prependToEventsFile(Collection<FileChangeEvent> changeEvents) {
		def events = changeEvents.findAll{ getMostRecentEventTime() == null || it.revisionDate >= getMostRecentEventTime() }
		if (events.empty) return events.size() == changeEvents.size()

		prependTo(filePath, toCsv(events))
		mostRecentEventTime = events.first().revisionDate

		events.size() == changeEvents.size()
	}

	Date getOldestEventTime() {
		if (oldestEventTime != null) return oldestEventTime
		def line = readLastLine(filePath)
		oldestEventTime = (line == null ? null :fromCsv(line).revisionDate)
	}

	Date getMostRecentEventTime() {
		if (mostRecentEventTime != null) return mostRecentEventTime
		def line = readFirstLine(filePath)
		mostRecentEventTime = (line == null ? null : fromCsv(line).revisionDate)
	}

	boolean hasNoEvents() {
		def file = new File(filePath)
		!file.exists() || file.length() == 0
	}

	private String toCsv(Collection<FileChangeEvent> changeEvents) {
		changeEvents.collect{ toCsv(it) }.join("\n") + "\n"
	}

	private String toCsv(FileChangeEvent changeEvent) {
		changeEvent.with {
			def stringWriter = new StringWriter()
			def csvWriter = new CSVWriter(stringWriter)
			([dateFormat.format(revisionDate), revision, author, fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType,
					lines.before, lines.after, lines.added, lines.modified, lines.removed,
					chars.before, chars.after, chars.added, chars.modified, chars.removed,
					escapeNewLines(commitMessage)] + additionalAttributes).each { csvWriter.writeField(String.valueOf(it)) }
			csvWriter.close()
			stringWriter.toString()
		}
	}


	private FileChangeEvent fromCsv(String line) {
		def fields = []
		new CSVReader().readFields(line, fields)
		def (revisionDate, revision, author, fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType,
				linesBefore, linesAfter, linesAdded, linesModified, linesRemoved,
				charsBefore, charsAfter, charsAdded, charsModified, charsRemoved, commitMessage
		) = fields
		revisionDate = dateFormat.parse(revisionDate)

		def additionalAttributes = fields.drop(19)

		def event = new FileChangeEvent(
				new CommitInfo(revision, author, revisionDate, unescapeNewLines(commitMessage)),
				new FileChangeInfo(fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType,
						new ChangeStats(asInt(linesBefore), asInt(linesAfter), asInt(linesAdded), asInt(linesModified), asInt(linesRemoved)),
						new ChangeStats(asInt(charsBefore), asInt(charsAfter), asInt(charsAdded), asInt(charsModified), asInt(charsRemoved))
				),
				additionalAttributes
		)
		event
	}

	private static escapeNewLines(String s) { s.replaceAll("\n", "\\\\n") }
	private static unescapeNewLines(String s) { s.replaceAll("\\\\n", "\n") }

	private static int asInt(String s) { s.toInteger() }


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
