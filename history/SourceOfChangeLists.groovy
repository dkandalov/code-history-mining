package history
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import history.util.PastToPresentIterator
import history.util.PresentToPastIterator

import static history.util.Measure.measure

class SourceOfChangeLists {
	static CommittedChangeList NO_MORE_CHANGE_LISTS = null

	private final Project project
	private final int sizeOfVCSRequestInDays

	SourceOfChangeLists(Project project, int sizeOfVCSRequestInDays = 30) {
		this.project = project
		this.sizeOfVCSRequestInDays = sizeOfVCSRequestInDays
	}

	Iterator<CommittedChangeList> fetchChangeLists(Date historyStart, Date historyEnd, boolean presentToPast = true) {
		assert historyStart.time < historyEnd.time

		Iterator dateIterator = (presentToPast ?
			new PresentToPastIterator(historyStart, historyEnd, sizeOfVCSRequestInDays) :
			new PastToPresentIterator(historyStart, historyEnd, sizeOfVCSRequestInDays)
		)
		List<CommittedChangeList> changes = []
		new Iterator<CommittedChangeList>() {
			@Override boolean hasNext() {
				!changes.empty || dateIterator.hasNext()
			}

			@Override CommittedChangeList next() {
				if (!changes.empty) return changes.remove(0)

				measure("VCS request time") {
					while (changes.empty && dateIterator.hasNext()) {
						def dates = dateIterator.next()
						changes = requestChangeListsFor(project, dates.from, dates.to)
						if (!presentToPast) changes = changes.reverse()
					}
				}
				changes.empty ? NO_MORE_CHANGE_LISTS : changes.remove(0)
			}

			@Override void remove() {
				throw new UnsupportedOperationException()
			}
		}
	}

	private static List<CommittedChangeList> requestChangeListsFor(Project project, Date fromDate = null, Date toDate = null) {
		def sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots.toList()
		def sourceRoot = sourceRoots.first()
		def vcsRoot = ProjectLevelVcsManager.getInstance(project).getVcsRootObjectFor(sourceRoot)
		if (vcsRoot == null) return []

		def changesProvider = vcsRoot.vcs.committedChangesProvider
		def location = changesProvider.getLocationFor(FilePathImpl.create(vcsRoot.path))

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

}
