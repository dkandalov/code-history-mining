package codehistoryminer.plugin.vcsaccess.implementation

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
import org.vcsreader.lang.TimeRange

class GitPluginWorkaround {
	/**
	 * Originally based on git4idea.changes.GitCommittedChangeListProvider#getCommittedChangesImpl
	 */
	static List<CommittedChangeList> requestCommits(Project project, RepositoryLocation location, TimeRange timeRange) {
		def result = []
		def parametersSpecifier = new Consumer<GitSimpleHandler>() {
			@Override void consume(GitSimpleHandler handler) {
				// makes git notice file renames/moves (not sure but seems that otherwise intellij api doesn't do it)
				handler.addParameters("-M")

				handler.addParameters("--after=" + String.valueOf(timeRange.from().epochSecond))
				handler.addParameters("--before=" + String.valueOf(timeRange.to().epochSecond))
			}
		}
		def resultConsumer = new Consumer<GitCommittedChangeList>() {
			@Override void consume(GitCommittedChangeList gitCommit) {
				result.add(gitCommit)
			}
		}
		VirtualFile root = LocalFileSystem.instance.findFileByIoFile(((GitRepositoryLocation) location).root)

		// if "false", Commit for merge will contain all changes from merge
		// this is NOT useful because changes will be in previous Commits anyway
		// TODO (not sure how it works with other VCS)
		boolean skipDiffsForMerge = true

		GitUtil.getLocalCommittedChanges(project, root, parametersSpecifier, resultConsumer, skipDiffsForMerge)

		result.findAll{ !isMergeCommit(it) }
	}

	private static isMergeCommit(CommittedChangeList changeList) {
		// Strictly speaking condition below is not correct since git allows commits with no changes.
		// This should be good enough though, because commits without changes are very rare.
		changeList.changes.empty
	}
}
