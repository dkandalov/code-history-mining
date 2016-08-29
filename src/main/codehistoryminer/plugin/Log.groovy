package codehistoryminer.plugin

import codehistoryminer.plugin.historystorage.HistoryStorage
import codehistoryminer.plugin.ui.UI
import codehistoryminer.plugin.vcsaccess.VcsActionsLog
import codehistoryminer.publicapi.lang.Date
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot
import org.vcsreader.lang.TimeRange

import java.time.format.DateTimeFormatter

import static java.time.ZoneOffset.UTC

class Log implements VcsActionsLog, HistoryStorage.Log, UI.Log, CodeHistoryMinerPluginLog {
	private final logger = Logger.getInstance("CodeHistoryMining")

	@Override def loadingProjectHistory(Date fromDate, Date toDate) {
		logger.info("Loading project history from ${fromDate} to ${toDate}")
	}

	@Override def cancelledBuilding(String visualizationName) {
		logger.info("Cancelled building '${visualizationName}'")
	}

	@Override def measuredDuration(def entry) {
		logger.info((String) entry.key + ": " + entry.value)
	}

	@Override errorReadingCommits(Exception e, TimeRange timeRange) {
		def formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(UTC)
		def from = formatter.format(timeRange.from())
		def to = formatter.format(timeRange.to())
		logger.warn("Error while reading commits from ${from} to ${to}", e)
	}

	@Override errorReadingCommits(String error) {
		logger.warn("Error while reading commits: ${error}")
	}

	@Override def failedToLocate(VcsRoot vcsRoot, Project project) {
		logger.warn("Failed to find location for ${vcsRoot} in ${project}")
	}

    @Override def onFailedToMineException(Throwable t) {
        logger.warn(t)
    }

    @Override def failedToMine(String message) {
        logger.warn("Filed to load file content: ${message}")
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
