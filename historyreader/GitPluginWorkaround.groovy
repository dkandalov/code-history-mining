package historyreader

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.RepositoryLocation
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Consumer
import git4idea.GitUtil
import git4idea.changes.GitCommittedChangeList
import git4idea.changes.GitRepositoryLocation
import git4idea.commands.GitSimpleHandler

class GitPluginWorkaround {
	/**
	 * Originally based on git4idea.changes.GitCommittedChangeListProvider#getCommittedChangesImpl
	 */
	static List<CommittedChangeList> getCommittedChanges_with_intellij_git_api_workarounds(Project project, RepositoryLocation location,
	                                                                                       Date fromDate = null, Date toDate = null) {
		def result = []
		def parametersSpecifier = new Consumer<GitSimpleHandler>() {
			@Override void consume(GitSimpleHandler h) {
				// makes git notice file renames/moves (not sure but seems that otherwise intellij api doesn't do it)
				h.addParameters("-M")

				if (toDate != null) h.addParameters("--before=" + GitUtil.gitTime(toDate));
				if (fromDate != null) h.addParameters("--after=" + GitUtil.gitTime(fromDate));
			}
		}
		def resultConsumer = new Consumer<GitCommittedChangeList>() {
			@Override void consume(GitCommittedChangeList gitCommit) {
				result << gitCommit
			}
		}
		VirtualFile root = LocalFileSystem.instance.findFileByIoFile(((GitRepositoryLocation) location).root)

		// if "false", Commit for merge will contain all changes from merge
		// this is NOT useful because changes will be in previous Commits anyway
		// TODO (not sure how it works with other VCS)
		boolean skipDiffsForMerge = true

		GitUtil.getLocalCommittedChanges(project, root, parametersSpecifier, resultConsumer, skipDiffsForMerge)

		def isNotMergeCommit = { CommittedChangeList commit -> commit.changes.size() > 0 }
		result.findAll{isNotMergeCommit(it)}
	}

}
