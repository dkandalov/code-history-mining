package vcsaccess.implementation
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CommittedChangesProvider
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import common.langutil.Measure
import common.langutil.PastToPresentIterator
import common.langutil.PresentToPastIterator
import org.jetbrains.annotations.Nullable
import vcsaccess.VcsAccessLog

class CommitReader {
	static Commit NO_MORE_COMMITS = null

	private final Project project
	private final Measure measure
	private final VcsAccessLog log
	private final int sizeOfVCSRequestInDays
	boolean lastRequestHadErrors

	CommitReader(Project project, int sizeOfVCSRequestInDays = 30, Measure measure = new Measure(), @Nullable VcsAccessLog log = null) {
		this.project = project
		this.sizeOfVCSRequestInDays = sizeOfVCSRequestInDays
		this.measure = measure
		this.log = log
	}

	Iterator<Commit> readCommits(Date historyStartDate, Date historyEndDate, boolean isReadingPresentToPast = true, List<VcsRoot> vcsRoots) {
		assert historyStartDate.time < historyEndDate.time
		// in local timezone dates should not have hours, minutes, seconds
		// (checking only seconds because Date.time field is in UTC and can have non-zero hours and probably minutes)
		assert historyStartDate.time % (60 * 1000) == 0
		assert historyEndDate.time % (60 * 1000) == 0

		lastRequestHadErrors = false

		Iterator dateIterator = (isReadingPresentToPast ?
			new PresentToPastIterator(historyStartDate, historyEndDate, sizeOfVCSRequestInDays) :
			new PastToPresentIterator(historyStartDate, historyEndDate, sizeOfVCSRequestInDays))
		List<Commit> changes = []

        def commitReaderLog = log
		new Iterator<Commit>() {
			@Override boolean hasNext() {
				!changes.empty || dateIterator.hasNext()
			}

			@Override Commit next() {
				if (!changes.empty) return changes.pop()

				measure.measure("VCS request time") {
					while (changes.empty && dateIterator.hasNext()) {
						def dates = dateIterator.next()
						try {
							changes = requestCommitsFrom(vcsRoots, project, dates.from, dates.to)
						} catch (Exception e) {
							// this is to catch errors in VCS plugin implementation 
							// e.g. this one http://youtrack.jetbrains.com/issue/IDEA-105360
							commitReaderLog?.errorReadingCommits(e, dates.from, dates.to)
							lastRequestHadErrors = true
						}
						changes = changes.sort{ it.commitDate }
						if (!isReadingPresentToPast) changes = changes.reverse()
					}
				}
				changes.empty ? NO_MORE_COMMITS : changes.pop()
			}

			@Override void remove() {
				throw new UnsupportedOperationException()
			}
		}
	}

	private List<Commit> requestCommitsFrom(List<VcsRoot> vcsRoots, Project project, Date fromDate, Date toDate) {
		vcsRoots
            .collectMany{ root -> doRequestCommitsFor(root, project, fromDate, toDate) }
            .sort{ it.commitDate }
	}

	private List<Commit> doRequestCommitsFor(VcsRoot vcsRoot, Project project, Date fromDate, Date toDate) {
		def changesProvider = vcsRoot.vcs.committedChangesProvider
		def location = changesProvider.getLocationFor(FilePathImpl.create(vcsRoot.path))

		if (location == null) {
			log?.failedToLocate(vcsRoot, project)
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
