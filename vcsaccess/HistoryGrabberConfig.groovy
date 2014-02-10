package vcsaccess

import com.intellij.openapi.util.io.FileUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Immutable

import java.text.SimpleDateFormat

@Immutable
class HistoryGrabberConfig {
	Date from
	Date to
	String outputFilePath
	boolean grabChangeSizeInLines
	boolean grabOnVcsUpdate

	HistoryGrabberConfig withToDate(Date newToDate) {
		new HistoryGrabberConfig(from, newToDate, outputFilePath, grabChangeSizeInLines, grabOnVcsUpdate)
	}

	static HistoryGrabberConfig loadGrabberConfigFor(String projectName, String pathToFolder, Closure<HistoryGrabberConfig> createDefault) {
		def stateByProject = loadStateByProject(pathToFolder)
		def result = stateByProject.get(projectName)
		result != null ? result : createDefault()
	}

	static saveGrabberConfigOf(String projectName, String pathToFolder, HistoryGrabberConfig grabberConfig) {
		def stateByProject = loadStateByProject(pathToFolder)
		stateByProject.put(projectName, grabberConfig)
		FileUtil.writeToFile(new File(pathToFolder + "/dialog-state.json"), JsonOutput.toJson(stateByProject))
	}

	private static Map<String, HistoryGrabberConfig> loadStateByProject(String pathToFolder) {
		try {
			def parseDate = { new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(it) }
			def parseBoolean = { it == null ? false : Boolean.parseBoolean(it.toString()) }
			def toGrabberConfig = { map -> new HistoryGrabberConfig(
					parseDate(map.from),
					parseDate(map.to),
					map.outputFilePath,
					parseBoolean(map.grabChangeSizeInLines),
					parseBoolean(map.grabOnVcsUpdate)
			)}

			def json = FileUtil.loadFile(new File(pathToFolder + "/dialog-state.json"))
			new JsonSlurper().parseText(json).collectEntries{ [it.key, toGrabberConfig(it.value)] }
		} catch (IOException ignored) {
			[:]
		}
	}
}
