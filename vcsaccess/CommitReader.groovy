package vcsaccess
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CommittedChangesProvider
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import vcsaccess._private.GitPluginWorkaround
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

	Iterator<Commit> readCommits(Date historyStart, Date historyEnd, boolean readPresentToPast = true, List<VcsRoot> vcsRoots) {
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
				if (!changes.empty) return changes.pop()

				measure("VCS request time") {
					while (changes.empty && dateIterator.hasNext()) {
						def dates = dateIterator.next()
						try {
							changes = requestCommitsFrom(vcsRoots, project, dates.from, dates.to)
						} catch (Exception e) {
							// this is to catch errors in VCS plugin implementation 
							// e.g. this one http://youtrack.jetbrains.com/issue/IDEA-105360
              LOG.warn("Error while reading commits from ${dates.from} to ${dates.to}", e)
							lastRequestHadErrors = true
            }
						changes = changes.sort{ it.commitDate }
						if (!readPresentToPast) changes = changes.reverse()
					}
				}
				changes.empty ? NO_MORE_COMMITS : changes.pop()
			}

			@Override void remove() {
				throw new UnsupportedOperationException()
			}
		}
	}

	private List<Commit> requestCommitsFrom(List<VcsRoot> vcsRoots, Project project, Date fromDate = null, Date toDate = null) {
		vcsRoots
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

	private static boolean isGit(CommittedChangesProvider changesProvider) {
		changesProvider.class.simpleName == "GitCommittedChangeListProvider"
	}
}
