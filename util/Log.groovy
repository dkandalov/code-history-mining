package util

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot

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

	def errorReadingCommits(Exception e, Date fromDate, Date toDate) {
		logger.warn("Error while reading commits from ${fromDate} to ${toDate}", e)
	}

	def failedToLocate(VcsRoot vcsRoot, Project project) {
		logger.warn("Failed to find location for ${vcsRoot} in ${project}")
	}

	def httpServerIsAboutToLoadHtmlFile(String fileName) {
		logger.info("Loading html file from: ${fileName}")
	}

	def errorOnHttpRequest(String message) {
		logger.info(message)
	}
}
