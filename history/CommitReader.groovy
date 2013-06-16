package history
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.RepositoryLocation
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import git4idea.GitUtil
import git4idea.changes.GitCommittedChangeList as GitCommit
import git4idea.changes.GitRepositoryLocation
import git4idea.commands.GitSimpleHandler
import history.util.PastToPresentIterator
import history.util.PresentToPastIterator

import static history.util.Measure.measure

class CommitReader {
	static Commit NO_MORE_CHANGE_LISTS = null

	private final Project project
	private final int sizeOfVCSRequestInDays

	CommitReader(Project project, int sizeOfVCSRequestInDays = 30) {
		this.project = project
		this.sizeOfVCSRequestInDays = sizeOfVCSRequestInDays
	}

	Iterator<Commit> readCommits(Date historyStart, Date historyEnd, boolean readPresentToPast = true) {
		assert historyStart.time < historyEnd.time

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
						changes = requestCommitsFor(project, dates.from, dates.to)
						if (!readPresentToPast) changes = changes.reverse()
					}
				}
				changes.empty ? NO_MORE_CHANGE_LISTS : changes.remove(0)
			}

			@Override void remove() {
				throw new UnsupportedOperationException()
			}
		}
	}

	static List<Commit> requestCommitsFor(Project project, Date fromDate = null, Date toDate = null) {
		def sourceRoots = ProjectRootManager.getInstance(project).contentSourceRoots.toList()
		def sourceRoot = sourceRoots.first()
		def vcsRoot = ProjectLevelVcsManager.getInstance(project).getVcsRootObjectFor(sourceRoot)
		if (vcsRoot == null) return []

		def changesProvider = vcsRoot.vcs.committedChangesProvider
		def location = changesProvider.getLocationFor(FilePathImpl.create(vcsRoot.path))
		if (changesProvider.class.simpleName == "GitCommittedChangeListProvider") {
			return workarounds_for_intellij_git_api(project, location, fromDate, toDate)
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
	 * Originally based on git4idea.changes.GitCommittedChangeListProvider#getCommittedChangesImpl
	 */
	private static List<Commit> workarounds_for_intellij_git_api(Project project, RepositoryLocation location, Date fromDate = null, Date toDate = null) {
		def result = []
		def parametersSpecifier = new Consumer<GitSimpleHandler>() {
			@Override void consume(GitSimpleHandler h) {
				// makes git notice file renames/moves (not sure but seems that otherwise intellij api doesn't do it)
				h.addParameters("-M")

				if (toDate != null) h.addParameters("--before=" + GitUtil.gitTime(toDate));
				if (fromDate != null) h.addParameters("--after=" + GitUtil.gitTime(fromDate));
			}
		}
		def resultConsumer = new Consumer<GitCommit>() {
			@Override void consume(GitCommit gitCommit) {
				result << gitCommit
			}
		}
		VirtualFile root = LocalFileSystem.instance.findFileByIoFile(((GitRepositoryLocation) location).root)

		// if "false", Commit for merge will contain all changes from merge
		// this is NOT useful because changes will be in previous Commits anyway
		// TODO (not sure how it works with other VCS)
		boolean skipDiffsForMerge = true

		GitUtil.getLocalCommittedChanges(project, root, parametersSpecifier, resultConsumer, skipDiffsForMerge)

		def isNotMergeCommit = { Commit commit -> commit.changes.size() > 0 }
		result.findAll{isNotMergeCommit(it)}
	}

	static amountOfVCSIn(Project project) {
		ProjectRootManager.getInstance(project).contentSourceRoots
				.collect{ ProjectLevelVcsManager.getInstance(project).getVcsRootObjectFor(it) }
				.findAll{ it.path != null }.unique()
				.size()
	}
}
