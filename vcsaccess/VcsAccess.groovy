package vcsaccess

import com.intellij.openapi.project.Project

class VcsAccess {
	boolean noVCSRootsIn(Project project) {
		ChangeEventsReader.noVCSRootsIn(project)
	}

	def changeEventsReaderFor(Project project, boolean grabChangeSizeInLines) {
		def vcsRequestBatchSizeInDays = 1 // based on personal observation (hardcoded so that not to clutter UI dialog)
		new ChangeEventsReader(
				project,
				new CommitReader(project, vcsRequestBatchSizeInDays),
				new CommitFilesMunger(project, grabChangeSizeInLines).&mungeCommit
		)
	}
}
