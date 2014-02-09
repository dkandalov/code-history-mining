package util

import com.intellij.openapi.diagnostic.Logger

class Log {
	private final logger = Logger.getInstance("CodeHistoryMining")

	def loadingProjectHistory(Date fromDate, Date toDate) {
		logger.info("Loading project history from ${fromDate} to ${toDate}")
	}

	def processingChangeList(String changeListName) {
		logger.info(changeListName)
	}

	def failedToRead(def line) {
		logger.warn("Failed to parse line '${line}'")
	}

	def cancelledBuilding(String visualizationName) {
		logger.info("Cancelled building '${visualizationName}'")
	}
}
