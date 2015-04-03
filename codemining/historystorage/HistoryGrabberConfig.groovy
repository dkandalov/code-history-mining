package codemining.historystorage
import codemining.core.common.langutil.Date2
import codemining.core.common.langutil.Time
import com.intellij.openapi.util.io.FileUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Immutable

@Immutable(knownImmutableClasses = [Date2, Time])
class HistoryGrabberConfig {
	Date2 from
	Date2 to
	String outputFilePath
	boolean grabChangeSizeInLines
	boolean grabOnVcsUpdate
	Time lastGrabTime

	HistoryGrabberConfig withLastGrabTime(Time updatedLastGrabTime) {
		new HistoryGrabberConfig(from, to, outputFilePath, grabChangeSizeInLines, grabOnVcsUpdate, updatedLastGrabTime)
	}

	HistoryGrabberConfig withOutputFilePath(String newOutputFilePath) {
		new HistoryGrabberConfig(from, to, newOutputFilePath, grabChangeSizeInLines, grabOnVcsUpdate, lastGrabTime)
	}

	static defaultConfig() {
		new HistoryGrabberConfig(Date2.today().shiftDays(-300), Date2.today(), "", false, false, Time.zero())
	}

	static HistoryGrabberConfig loadGrabberConfigFor(String projectName, String pathToFolder, Closure<HistoryGrabberConfig> createDefault) {
		def stateByProject = loadStateByProject(pathToFolder)
		def result = stateByProject.get(projectName)
		result != null ? result : createDefault()
	}

	static saveGrabberConfigOf(String projectName, String pathToFolder, HistoryGrabberConfig grabberConfig) {
		def stateByProject = loadStateByProject(pathToFolder)
		stateByProject.put(projectName, grabberConfig)
		FileUtil.writeToFile(new File(pathToFolder + "/grabber-config.json"), JsonOutput.toJson(stateByProject))

		def oldFile = new File(pathToFolder + "/dialog-state.json")
		if (oldFile.exists()) FileUtil.delete(oldFile)
	}

	private static Map<String, HistoryGrabberConfig> loadStateByProject(String pathToFolder) {
		try {
			def parseBoolean = { Boolean.parseBoolean(it?.toString()) }
			def toGrabberConfig = { map -> new HistoryGrabberConfig(
					parseDate(map.from),
					parseDate(map.to),
					map.outputFilePath,
					parseBoolean(map.grabChangeSizeInLines),
					parseBoolean(map.grabOnVcsUpdate),
					parseTime(map.lastGrabTime)
			)}

			def json = readConfigFile(pathToFolder)
			new JsonSlurper().parseText(json).collectEntries{ [it.key, toGrabberConfig(it.value)] }

		} catch (Exception ignored) {
			[:]
		}
	}

	private static Date2 parseDate(String s) {
		def defaultDate = new Date2(new Date(0))
		try {
			s == null ? defaultDate : Date2.Formatter.ISO1806.parse(s)
		} catch (Exception ignored) {
			defaultDate
		}
	}

	private static Time parseTime(String s) {
		try {
			s == null ? Time.zero() : Time.Formatter.ISO1806.parse(s)
		} catch (Exception ignored) {
			Time.zero()
		}
	}

	private static String readConfigFile(String pathToFolder) {
		def oldFile = new File(pathToFolder + "/dialog-state.json")
		if (oldFile.exists()) {
			FileUtil.loadFile(oldFile)
		} else {
			FileUtil.loadFile(new File(pathToFolder + "/grabber-config.json"))
		}
	}
}
