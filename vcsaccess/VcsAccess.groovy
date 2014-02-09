package vcsaccess

import com.intellij.openapi.project.Project
import util.Log

class VcsAccess {
	private final Log log

	VcsAccess(Log log) {
		this.log = log
	}

	boolean noVCSRootsIn(Project project) {
		ChangeEventsReader.noVCSRootsIn(project)
	}

	def changeEventsReaderFor(Project project, boolean grabChangeSizeInLines) {
		def vcsRequestBatchSizeInDays = 1 // based on personal observation (hardcoded so that not to clutter UI dialog)
		new ChangeEventsReader(
				project,
				new CommitReader(project, log, vcsRequestBatchSizeInDays),
				new CommitFilesMunger(project, grabChangeSizeInLines).&mungeCommit
		)
	}
}
