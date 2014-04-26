package miner

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot
import historystorage.HistoryStorage
import miner.ui.UI
import vcsaccess.VcsAccessLog

class Log implements VcsAccessLog, Miner.Log, HistoryStorage.Log, UI.Log {
	private final logger = Logger.getInstance("CodeHistoryMining")

	@Override def loadingProjectHistory(Date fromDate, Date toDate) {
		logger.info("Loading project history from ${fromDate} to ${toDate}")
	}

	@Override def processingChangeList(String changeListName) {
		logger.info(changeListName)
	}

	@Override def cancelledBuilding(String visualizationName) {
		logger.info("Cancelled building '${visualizationName}'")
	}

	@Override def measuredDuration(def entry) {
		logger.info(entry.key + ": " + entry.value)
	}

	@Override errorReadingCommits(Exception e, Date fromDate, Date toDate) {
		logger.warn("Error while reading commits from ${fromDate} to ${toDate}", e)
	}

	@Override def failedToLocate(VcsRoot vcsRoot, Project project) {
		logger.warn("Failed to find location for ${vcsRoot} in ${project}")
	}

	@Override def failedToRead(def line) {
		logger.warn("Failed to parse line '${line}'")
	}

	@Override def httpServerIsAboutToLoadHtmlFile(String fileName) {
		logger.info("Loading html file from: ${fileName}")
	}

	@Override def errorOnHttpRequest(String message) {
		logger.info(message)
	}
}
