package codehistoryminer.plugin.vcsaccess.implementation
import codehistoryminer.core.common.langutil.Date
import codehistoryminer.plugin.vcsaccess.VcsActionsLog
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.CommittedChangesProvider
import com.intellij.openapi.vcs.LocalFilePath
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import org.jetbrains.annotations.Nullable

class IJCommitReader {
	private final Project project
	private final VcsActionsLog log
	boolean lastRequestHadErrors

	IJCommitReader(Project project, @Nullable VcsActionsLog log = null) {
		this.project = project
		this.log = log
	}

	List<Commit> readCommits(Date historyStartDate, Date historyEndDate, List<VcsRoot> vcsRoots) {
		assert historyStartDate.before(historyEndDate)

		lastRequestHadErrors = false

        List<Commit> changes = []
        try {
            changes = requestCommitsFrom(vcsRoots, project, historyStartDate, historyEndDate)
        } catch (Exception e) {
            // this is to catch errors in VCS plugin implementation
            // e.g. this one http://youtrack.jetbrains.com/issue/IDEA-105360
            log?.errorReadingCommits(e, historyStartDate, historyEndDate)
            lastRequestHadErrors = true
        }
        changes
	}

	private List<Commit> requestCommitsFrom(List<VcsRoot> vcsRoots, Project project, Date fromDate, Date toDate) {
		vcsRoots
            .collectMany{ root -> doRequestCommitsFor(root, project, fromDate, toDate) }
            .sort{ it.commitDate }
	}

	private List<Commit> doRequestCommitsFor(VcsRoot vcsRoot, Project project, Date fromDate, Date toDate) {
		def changesProvider = vcsRoot.vcs.committedChangesProvider
		def location = changesProvider.getLocationFor(new LocalFilePath(vcsRoot.path.canonicalPath, true))

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
			settings.dateAfter = fromDate.javaDate()
		}
		if (toDate != null) {
			settings.USE_DATE_BEFORE_FILTER = true
			settings.dateBefore = toDate.javaDate()
		}
		changesProvider.getCommittedChanges(settings, location, changesProvider.unlimitedCountValue)
	}

	private static boolean isGit(CommittedChangesProvider changesProvider) {
		changesProvider.class.simpleName == "GitCommittedChangeListProvider"
	}
}
