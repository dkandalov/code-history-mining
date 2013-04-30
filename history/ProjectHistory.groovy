package history

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.RepositoryLocation
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import git4idea.GitUtil
import git4idea.changes.GitCommittedChangeList
import git4idea.changes.GitRepositoryLocation
import git4idea.commands.GitSimpleHandler
import history.util.PastToPresentIterator
import history.util.PresentToPastIterator

import static history.util.Measure.measure


class ProjectHistory {

	static Iterator<CommittedChangeList> fetchChangeListsFor(Project project, Date historyStart, Date historyEnd,
	                                                    int sizeOfVCSRequestInDays = 30, boolean presentToPast = true) {
		Iterator dateIterator
		if (presentToPast) dateIterator = new PresentToPastIterator(historyStart, historyEnd, sizeOfVCSRequestInDays)
		else dateIterator = new PastToPresentIterator(historyStart, historyEnd, sizeOfVCSRequestInDays)
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
				changes.empty ? null : changes.remove(0)
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
		if (changesProvider.class.simpleName == "GitCommittedChangeListProvider") {
			return bug_IDEA_102084(project, location, fromDate, toDate)
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

	/**
	 * see http://youtrack.jetbrains.com/issue/IDEA-102084
	 * this issue is fixed, left this workaround anyway to have backward compatibility with IJ12 releases before the fix
	 */
	private static List<CommittedChangeList> bug_IDEA_102084(Project project, RepositoryLocation location, Date fromDate = null, Date toDate = null) {
		def result = []
		def parametersSpecifier = new Consumer<GitSimpleHandler>() {
			@Override void consume(GitSimpleHandler h) {
				// makes git notice file renames/moves (not sure but seems that otherewise intellij api doesn't do it)
				h.addParameters("-M")

				if (toDate != null) h.addParameters("--before=" + GitUtil.gitTime(toDate));
				if (fromDate != null) h.addParameters("--after=" + GitUtil.gitTime(fromDate));
			}
		}
		def resultConsumer = new Consumer<GitCommittedChangeList>() {
			@Override void consume(GitCommittedChangeList changeList) {
				result << changeList
			}
		}
		VirtualFile root = LocalFileSystem.instance.findFileByIoFile(((GitRepositoryLocation) location).root)

		// this is another difference compared to GitCommittedChangeListProvider#getCommittedChangesImpl
		// if "false", merge CommittedChangeList will contain all changes from merge which is NOT useful for this use-case
		// TODO (not sure how it works with other VCS)
		boolean skipDiffsForMerge = true

		GitUtil.getLocalCommittedChanges(project, root, parametersSpecifier, resultConsumer, skipDiffsForMerge)
		result
	}
}
