package codehistoryminer.plugin.vcsaccess.implementation

import codehistoryminer.plugin.vcsaccess.VcsActionsLog
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CommittedChangesProvider
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import org.jetbrains.annotations.Nullable
import org.vcsreader.lang.TimeRange

class IJCommitReader {
	private final Project project
	private final VcsActionsLog log
	boolean lastRequestHadErrors

	IJCommitReader(Project project, @Nullable VcsActionsLog log = null) {
		this.project = project
		this.log = log
	}

	List<Commit> readCommits(TimeRange timeRange, List<VcsRoot> vcsRoots) {
		lastRequestHadErrors = false

        List<Commit> changes = []
        try {
            changes = requestCommitsFrom(vcsRoots, project, timeRange)
        } catch (Exception e) {
            // this is to catch errors in VCS plugin implementation
            // e.g. this one http://youtrack.jetbrains.com/issue/IDEA-105360
            log?.errorReadingCommits(e, timeRange)
            lastRequestHadErrors = true
        }
        changes
	}

	private List<Commit> requestCommitsFrom(List<VcsRoot> vcsRoots, Project project, TimeRange timeRange) {
		vcsRoots
            .collectMany{ root -> doRequestCommitsFor(root, project, timeRange) }
            .sort{ it.commitDate }
	}

	private List<Commit> doRequestCommitsFor(VcsRoot vcsRoot, Project project, TimeRange timeRange) {
		def changesProvider = vcsRoot.vcs.committedChangesProvider
		def location = changesProvider.getLocationFor(new LocalFilePath(vcsRoot.path.canonicalPath, true))

		if (location == null) {
			log?.failedToLocate(vcsRoot, project)
			lastRequestHadErrors = true
			return []
		}

		if (isGit(changesProvider)) {
			return GitPluginWorkaround.requestCommits(project, location, timeRange)
		}

		def settings = changesProvider.createDefaultSettings()
		settings.USE_DATE_AFTER_FILTER = true
		settings.dateAfter = new Date(timeRange.from().toEpochMilli())
		settings.USE_DATE_BEFORE_FILTER = true
		settings.dateBefore = new Date(timeRange.to().toEpochMilli())

		changesProvider.getCommittedChanges(settings, location, changesProvider.unlimitedCountValue)
	}

	private static boolean isGit(CommittedChangesProvider changesProvider) {
		changesProvider.class.simpleName == "GitCommittedChangeListProvider"
	}
}
