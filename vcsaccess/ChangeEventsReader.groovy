package vcsaccess
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import common.events.FileChangeEvent
import liveplugin.PluginUtil
import vcsaccess.implementation.CommitReader

import static common.langutil.DateTimeUtil.floorToDay

class ChangeEventsReader {
	private static final Closure DEFAULT_WRAPPER = { changes, aCallback -> aCallback(changes) }

	private final Project project
	private final CommitReader commitReader
	private final def extractChangeEvents

	ChangeEventsReader(Project project = null, CommitReader commitReader = null, Closure<Collection<FileChangeEvent>> extractChangeEvents = null) {
		this.project = project
		this.commitReader = commitReader
		this.extractChangeEvents = extractChangeEvents
	}

	def readPresentToPast(Date historyStart, Date historyEnd, Closure isCancelled = null,
	                      Closure consumeWrapper = DEFAULT_WRAPPER, Closure consume) {
		request(historyStart, historyEnd, isCancelled, true, consumeWrapper, consume)
	}

	def readPastToPresent(Date historyStart, Date historyEnd, Closure isCancelled = null,
	                      Closure consumeWrapper = DEFAULT_WRAPPER, Closure consume) {
		request(historyStart, historyEnd, isCancelled, false, consumeWrapper, consume)
	}

	private request(Date historyStart, Date historyEnd, Closure isCancelled = null, boolean readPresentToPast,
	            Closure consumeWrapper, Closure consume) {
		def fromDate = floorToDay(historyStart)
		def toDate = floorToDay(historyEnd) + 1 // +1 because commitReader end date is exclusive

		Iterator<Commit> commits = commitReader.readCommits(fromDate, toDate, readPresentToPast, vcsRootsIn(project))

		for (commit in commits) {
			if (commit == CommitReader.NO_MORE_COMMITS) break
			if (isCancelled?.call()) break
			if (commit.commitDate < historyStart || commit.commitDate > historyEnd) continue
			if (readPresentToPast && commit.commitDate == historyEnd) continue
			if (!readPresentToPast && commit.commitDate == historyStart) continue

			consumeWrapper(commit) {
				PluginUtil.catchingAll {
					consume(extractChangeEvents(commit))
				}
			}
		}
	}

	boolean getLastRequestHadErrors() {
		commitReader.lastRequestHadErrors
	}

	static List<VcsRoot> vcsRootsIn(Project project) {
		ProjectLevelVcsManager.getInstance(project).allVcsRoots
	}

	static boolean noVCSRootsIn(Project project) {
		vcsRootsIn(project).size() == 0
	}
}
