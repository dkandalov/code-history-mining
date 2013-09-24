package events
import com.intellij.openapi.util.io.FileUtil
import events.csv4180.CSVReader
import events.csv4180.CSVWriter
import groovy.transform.CompileStatic
import util.Measure

import java.text.SimpleDateFormat

import static util.CancelledException.check

class EventStorage {
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z")

	final String filePath

	EventStorage(String filePath) {
		this.filePath = filePath
	}

	@CompileStatic
	List<FileChangeEvent> readAllEvents(indicator = null, Closure whenFiledToParseLine = {line, e ->}) {
		def events = []
		def reader = new InputStreamReader(new BufferedInputStream(new FileInputStream(filePath)))
		try {
			def line
			while ((line = reader.readLine()) != null) {
				try {
					events << fromCsv(line)
				} catch (Exception e) {
					whenFiledToParseLine(line, e)
				}
				check(indicator)
			}
		} finally {
			reader.close()
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

	private static String toCsv(FileChangeEvent changeEvent) {
		changeEvent.with {
			def stringWriter = new StringWriter()
			def csvWriter = new CSVWriter(stringWriter)
			[format(revisionDate), revision, author, fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType,
					lines.before, lines.after, lines.added, lines.modified, lines.removed,
					chars.before, chars.after, chars.added, chars.modified, chars.removed,
					escapeNewLines(commitMessage)].each { csvWriter.writeField(String.valueOf(it)) }
			csvWriter.close()
			stringWriter.toString()
		}
	}

	private static FileChangeEvent fromCsv(String line) {
		def fields = []
		Measure.measure("csvReader.readFields") {
			def csvReader = new CSVReader(new StringReader(line))
			csvReader.readFields(fields)
		}
		def (revisionDate, revision, author, fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType,
				linesBefore, linesAfter, linesAdded, linesModified, linesRemoved,
				charsBefore, charsAfter, charsAdded, charsModified, charsRemoved, commitMessage
		) = fields
		revisionDate = DATE_FORMAT.parse(revisionDate)

		def event = new FileChangeEvent(
				new CommitInfo(revision, author, revisionDate, unescapeNewLines(commitMessage)),
				new FileChangeInfo(fileNameBefore, fileName, packageNameBefore, packageName, fileChangeType,
						new ChangeStats(asInt(linesBefore), asInt(linesAfter), asInt(linesAdded), asInt(linesModified), asInt(linesRemoved)),
						new ChangeStats(asInt(charsBefore), asInt(charsAfter), asInt(charsAdded), asInt(charsModified), asInt(charsRemoved))
				)
		)
		event
	}

	private static escapeNewLines(String s) { s.replaceAll("\n", "\\\\n") }
	private static unescapeNewLines(String s) { s.replaceAll("\\\\n", "\n") }

	private static int asInt(String s) { s.toInteger() }

	private static String format(Date date) {
		DATE_FORMAT.format(date)
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
