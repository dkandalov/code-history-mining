import com.intellij.openapi.application.PathManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import history.*
import history.events.ChangeEvent
import history.events.CommitInfo
import history.events.ElementChangeInfo
import history.events.FileChangeInfo
import org.jetbrains.annotations.Nullable

import java.text.SimpleDateFormat

import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static history.ChangeExtractor.changeEventsFrom
import static history.Measure.measure
import static intellijeval.PluginUtil.*

if (isIdeStartup) return

//new TextCompareProcessorTestSuite(project).run()
//if (true) return


doInBackground("Analyzing project history", { ProgressIndicator indicator ->
	measure("time") {
		def storage = new EventStorage("${PathManager.pluginsPath}/delta-flora/${project.name}-events.csv")

		def now = new Date()
		def daysOfHistory = 900
		def sizeOfVCSRequestInDays = 1

		if (storage.hasNoEvents()) {
			def historyStart = now - daysOfHistory
			def historyEnd = now

			log("Loading project history from $historyStart to $historyEnd")
			Iterator<CommittedChangeList> changeLists = ProjectHistory.fetchChangeListsFor(project, historyStart, historyEnd, sizeOfVCSRequestInDays)
			processChangeLists(changeLists, indicator) { changeEvents ->
				storage.appendToEventsFile(changeEvents)
			}
		} else {
			def historyStart = storage.mostRecentEventTime
			def historyEnd = now
			log("Loading project history from $historyStart to $historyEnd")

			def changeLists = ProjectHistory.fetchChangeListsFor(project, historyStart, historyEnd, sizeOfVCSRequestInDays, false)
			processChangeLists(changeLists, indicator) { changeEvents ->
				storage.prependToEventsFile(changeEvents)
			}

			historyStart = now - daysOfHistory
			historyEnd = storage.oldestEventTime
			log("Loading project history from $historyStart to $historyEnd")

			changeLists = ProjectHistory.fetchChangeListsFor(project, historyStart, historyEnd, sizeOfVCSRequestInDays)
			processChangeLists(changeLists, indicator) { changeEvents ->
				storage.appendToEventsFile(changeEvents)
			}
		}

		showInConsole("Saved change events to ${storage.filePath}", "output", project)
		showInConsole("(it should have history from '${storage.oldestEventTime}' to '${storage.mostRecentEventTime}')", "output", project)
	}
	Measure.durations.entrySet().collect{ "Total " + it.key + ": " + it.value }.each{ log(it) }
}, {})

def processChangeLists(changeLists, indicator, callback) {
	for (changeList in changeLists) {
		if (changeList == null) break
		if (indicator.canceled) break
		log(changeList.name)

		def date = dateFormat.format((Date) changeList.commitDate)
		indicator.text = "Analyzing project history (${date} - '${changeList.comment.trim()}')"
		catchingAll_ {
//			Collection<ChangeEvent> changeEvents = fileChangeEventsFrom((CommittedChangeList) changeList, project)
			Collection<ChangeEvent> changeEvents = changeEventsFrom((CommittedChangeList) changeList, project)
			callback(changeEvents)
		}
		indicator.text = "Analyzing project history (${date} - looking for next commit...)"
	}
}

@Nullable static <T> T catchingAll_(Closure<T> closure) {
	try {
		closure.call()
	} catch (Exception e) {
		log(e)
		null
	}
}


class EventStorage {
	static final String CSV_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss Z"

	final String filePath

	EventStorage(String filePath) {
		this.filePath = filePath
	}

	List<ChangeEvent> readAllEvents() {
		def events = []
		new File(filePath).withReader { reader ->
			def line = null
			while ((line = reader.readLine()) != null) {
				try {
					events << fromCsv(line)
				} catch (Exception ignored) {
					println("Failed to parse line '${line}'")
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
			[format(revisionDate), revision, author, elementName.replaceAll(",", ""),
					fileName, fileChangeType, packageBefore, packageAfter, linesInFileBefore, linesInFileAfter,
					changeType, fromLine, toLine, fromOffset, toOffset, commitMessageEscaped].join(",")
		}
	}

	private static ChangeEvent fromCsv(String line) {
		def (revisionDate, revision, author, elementName,
				fileName, fileChangeType, packageBefore, packageAfter, linesInFileBefore, linesInFileAfter,
				changeType, fromLine, toLine, fromOffset, toOffset) = line.split(",")
		revisionDate = new SimpleDateFormat(CSV_DATE_FORMAT).parse(revisionDate)
		def commitMessage = line.substring(line.indexOf('"') + 1, line.size() - 1)

		def event = new ChangeEvent(
				new CommitInfo(revision, author, revisionDate, commitMessage),
				new FileChangeInfo(fileName, fileChangeType, packageBefore, packageAfter, linesInFileBefore.toInteger(), linesInFileAfter.toInteger()),
				new ElementChangeInfo(elementName, changeType, fromLine.toInteger(), toLine.toInteger(), fromOffset.toInteger(), toOffset.toInteger())
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


