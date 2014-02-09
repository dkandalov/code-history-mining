package vcsaccess

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nullable
import util.Log
import util.Measure

class VcsAccess {
	private final Measure measure
	private final Log log

	VcsAccess(Measure measure = null, @Nullable Log log = null) {
		this.measure = measure
		this.log = log
	}

	boolean noVCSRootsIn(Project project) {
		ChangeEventsReader.noVCSRootsIn(project)
	}

	def changeEventsReaderFor(Project project, boolean grabChangeSizeInLines) {
		def vcsRequestBatchSizeInDays = 1 // based on personal observation (hardcoded so that not to clutter UI dialog)
		new ChangeEventsReader(
				project,
				new CommitReader(project, vcsRequestBatchSizeInDays, measure, log),
				new CommitFilesMunger(project, grabChangeSizeInLines).&mungeCommit
		)
	}

	def addVcsUpdateListenerFor(String projectName, Closure closure) {
		// TODO implement
	}

	def removeVcsUpdateListenerFor(String projectName) {
		// TODO implement
	}
}
