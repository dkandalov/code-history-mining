package codemining.historystorage
import codemining.core.common.langutil.Date
import codemining.core.common.langutil.Time
import com.intellij.openapi.util.io.FileUtil
import groovy.json.JsonSlurper
import groovy.transform.Immutable

@Immutable(knownImmutableClasses = [Date, Time])
class HistoryGrabberConfig {
	Date from
	Date to
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
		new HistoryGrabberConfig(Date.today().shiftDays(-300), Date.today(), "", false, false, Time.zero())
	}

	static HistoryGrabberConfig loadGrabberConfigFor(String projectName, String pathToFolder, Closure<HistoryGrabberConfig> createDefault) {
		def stateByProject = loadStateByProject(pathToFolder)
		def result = stateByProject.get(projectName)
		result != null ? result : createDefault()
	}

	static saveGrabberConfigOf(String projectName, String pathToFolder, HistoryGrabberConfig grabberConfig) {
		def stateByProject = loadStateByProject(pathToFolder)
		stateByProject.put(projectName, grabberConfig)

		FileUtil.writeToFile(new File(pathToFolder + "/grabber-config.json"), Serializer.stateToJson(stateByProject))

		def oldFile = new File(pathToFolder + "/dialog-state.json")
		if (oldFile.exists()) FileUtil.delete(oldFile)
	}

	private static Map<String, HistoryGrabberConfig> loadStateByProject(String pathToFolder) {
		try {
			Serializer.stateFromJson(readConfigFile(pathToFolder))
		} catch (Exception ignored) {
			[:]
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

	/*private*/ static class Serializer {
		static String stateToJson(Map<String, HistoryGrabberConfig> state) {
			def values = state.collect{ '"' + it.key + '":' + toJsonObjectString(it.value) }
			"{" + values.join(",") + "}"
		}

		static Map<String, HistoryGrabberConfig> stateFromJson(String json) {
			new JsonSlurper().parseText(json).collectEntries{ [it.key, fromJsonMap(it.value)] }
		}

		private static String toJsonObjectString(HistoryGrabberConfig config) {
			def values = [
					'"from":"' + Date.Formatter.ISO1806.format(config.from) + '"',
					'"to":"' + Date.Formatter.ISO1806.format(config.to) + '"',
					'"outputFilePath":"' + config.outputFilePath + '"',
					'"grabChangeSizeInLines":' + config.grabChangeSizeInLines,
					'"grabOnVcsUpdate":' + config.grabOnVcsUpdate,
					'"lastGrabTime":"' + Time.Formatter.ISO1806.format(config.lastGrabTime) + '"'
			]
			"{" + values.join(",") + "}"
		}

		private static HistoryGrabberConfig fromJsonMap(Map map) {
			def parseBoolean = { Boolean.parseBoolean(it?.toString()) }
			new HistoryGrabberConfig(
					parseDate(map.from),
					parseDate(map.to),
					map.outputFilePath,
					parseBoolean(map.grabChangeSizeInLines),
					parseBoolean(map.grabOnVcsUpdate),
					parseTime(map.lastGrabTime)
			)
		}

		private static Date parseDate(String s) {
			def defaultDate = Date.zero()
			try {
				s == null ? defaultDate : Date.Formatter.ISO1806.parse(s)
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
	}
}
