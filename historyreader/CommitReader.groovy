package historyreader
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.CommittedChangesProvider
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import util.PastToPresentIterator
import util.PresentToPastIterator

import static util.Measure.measure

class CommitReader {
	private static final Logger LOG = Logger.getInstance(CommitReader.class.name)
	static Commit NO_MORE_COMMITS = null

	private final Project project
	private final int sizeOfVCSRequestInDays
	boolean lastRequestHadErrors

	CommitReader(Project project, int sizeOfVCSRequestInDays = 30) {
		this.project = project
		this.sizeOfVCSRequestInDays = sizeOfVCSRequestInDays
	}

	Iterator<Commit> readCommits(Date historyStart, Date historyEnd, boolean readPresentToPast = true) {
		assert historyStart.time < historyEnd.time
		lastRequestHadErrors = false

		Iterator dateIterator = (readPresentToPast ?
			new PresentToPastIterator(historyStart, historyEnd, sizeOfVCSRequestInDays) :
			new PastToPresentIterator(historyStart, historyEnd, sizeOfVCSRequestInDays))
		List<Commit> changes = []

		new Iterator<Commit>() {
			@Override boolean hasNext() {
				!changes.empty || dateIterator.hasNext()
			}

			@Override Commit next() {
				if (!changes.empty) return changes.remove(0)

				measure("VCS request time") {
					while (changes.empty && dateIterator.hasNext()) {
						def dates = dateIterator.next()
						try {
							changes = requestCommitsFor(project, dates.from, dates.to)
						} catch (Exception e) {
							// this is to catch errors in VCS plugin implementation 
							// e.g. this one http://youtrack.jetbrains.com/issue/IDEA-105360
              LOG.warn("Error while reading commits from ${dates.from} to ${dates.to}", e)
							lastRequestHadErrors = true
            }
						if (!readPresentToPast) changes = changes.reverse()
					}
				}
				changes.empty ? NO_MORE_COMMITS : changes.remove(0)
			}

			@Override void remove() {
				throw new UnsupportedOperationException()
			}
		}
	}

	private List<Commit> requestCommitsFor(Project project, Date fromDate = null, Date toDate = null) {
		vcsRootsIn(project)
				.collectMany{ root -> doRequestCommitsFor(root, project, fromDate, toDate) }
				.sort{ it.commitDate }
	}

	private List<Commit> doRequestCommitsFor(VcsRoot vcsRoot, Project project, Date fromDate, Date toDate) {
		def changesProvider = vcsRoot.vcs.committedChangesProvider
		def location = isGit(changesProvider) ?
			GitPluginWorkaround.getGetLocation_with_intellij_git_api_workaround(vcsRoot) :
			changesProvider.getLocationFor(FilePathImpl.create(vcsRoot.path))

		if (location == null) {
			LOG.warn("Failed to find location for ${vcsRoot} in ${project}")
			lastRequestHadErrors = true
			return []
		}

		if (isGit(changesProvider)) {
			return GitPluginWorkaround.getCommittedChanges_with_intellij_git_api_workarounds(project, location, fromDate, toDate)
		}

		def settings = changesProvider.createDefaultSettings()
		if (fromDate != null) {
			settings.USE_DATE_AFTER_FILTER = true
			settings.dateAfter = fromDate
		}
		if (toDate != null) {
			settings.USE_DATE_BEFORE_FILTER = true
			settings.dateBefore = toDate
		}
		changesProvider.getCommittedChanges(settings, location, changesProvider.unlimitedCountValue)
	}

	static boolean noVCSRootsIn(Project project) {
		vcsRootsIn(project).size() == 0
	}

	private static List<VcsRoot> vcsRootsIn(Project project) {
		ProjectRootManager.getInstance(project).contentSourceRoots
				.collect{ ProjectLevelVcsManager.getInstance(project).getVcsRootObjectFor(it) }
				.findAll{ it.path != null }.unique()
	}

	private static boolean isGit(CommittedChangesProvider changesProvider) {
		changesProvider.class.simpleName == "GitCommittedChangeListProvider"
	}
}
