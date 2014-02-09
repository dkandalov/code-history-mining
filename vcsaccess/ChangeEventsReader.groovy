package vcsaccess
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import events.FileChangeEvent
import liveplugin.PluginUtil

class ChangeEventsReader {
	private static final Closure DEFAULT_WRAPPER = { changes, aCallback -> aCallback(changes) }

	private final CommitReader commitReader
	private final def extractChangeEvents
	private final List<VcsRoot> vcsRoots

	ChangeEventsReader(Project project, CommitReader commitReader, Closure<Collection<FileChangeEvent>> extractChangeEvents) {
		this.commitReader = commitReader
		this.extractChangeEvents = extractChangeEvents
		this.vcsRoots = vcsRootsIn(project)
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
		Iterator<Commit> commits = commitReader.readCommits(historyStart, historyEnd, readPresentToPast, vcsRoots)
		for (commit in commits) {
			if (commit == CommitReader.NO_MORE_COMMITS) break
			if (isCancelled?.call()) break

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
