package codehistoryminer.plugin.vcsaccess.implementation.wrappers

import codehistoryminer.plugin.vcsaccess.VcsActionsLog
import codehistoryminer.plugin.vcsaccess.implementation.IJCommitReader
import com.intellij.openapi.project.Project as IJProject
import com.intellij.openapi.vcs.VcsRoot as IJVcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as IJCommit
import com.intellij.vcs.log.VcsShortCommitDetails
import org.vcsreader.VcsChange
import org.vcsreader.VcsProject
import org.vcsreader.VcsRoot
import org.vcsreader.lang.TimeRange
import org.vcsreader.vcs.Commit

import static codehistoryminer.core.lang.Misc.withDefault

class VcsRootWrapper implements VcsRoot {
    private final IJProject project
    private final IJVcsRoot vcsRoot
    private final String commonVcsRoot
    private final VcsActionsLog log

    VcsRootWrapper(IJProject project, IJVcsRoot vcsRoot, String commonVcsRoot, VcsActionsLog log) {
        this.project = project
        this.vcsRoot = vcsRoot
        this.commonVcsRoot = commonVcsRoot
        this.log = log
    }

    @Override VcsProject.LogResult log(TimeRange timeRange) {
        def reader = new IJCommitReader(project, log)
        def commits = reader.readCommits(timeRange, [vcsRoot])

        def result = []
        for (IJCommit ijCommit in commits) {
            def revision = withDefault(VcsChange.noRevision, ijCommit.changes.first().afterRevision?.revisionNumber?.asString())
            def revisionBefore = withDefault(VcsChange.noRevision, ijCommit.changes.first().beforeRevision?.revisionNumber?.asString())

	        // workaround because hg4idea will use "revision:changeset" as id (using terms of hg)
	        if (ijCommit?.vcs?.name == "hg4idea") {
		        revision = keepHgChangeSetOnly(revision)
		        revisionBefore = keepHgChangeSetOnly(revision)
	        }

            def changes = wrapChangesFrom(ijCommit)
            if (changes.empty) continue

	        def commitTime
	        if (ijCommit instanceof VcsShortCommitDetails) {
		        commitTime = new java.util.Date(ijCommit.authorTime)
	        } else {
		        commitTime = ijCommit.commitDate
	        }
	        def commit = new Commit(
                    revision,
                    revisionBefore,
			        commitTime,
                    ijCommit.committerName,
                    ijCommit.comment.trim(),
                    changes
            )

            result.add(commit)
        }

        new VcsProject.LogResult(result, [])
    }

	private List<VcsChange> wrapChangesFrom(IJCommit ijCommit) {
        ijCommit.changes
            .collect { ChangeWrapper.create(it, commonVcsRoot) }
            .findAll{ it != ChangeWrapper.none }
    }

	private static String keepHgChangeSetOnly(String s) {
		def i = s.indexOf(":")
		if (i == -1 || i == s.length() - 1) return s
		s.substring(i + 1)
	}

    @Override VcsProject.LogFileContentResult logFileContent(String filePath, String revision) {
        throw new IllegalStateException("Method should never be called (filePath: ${filePath}; revision: ${revision})")
    }

    @Override VcsProject.UpdateResult update() {
        throw new UnsupportedOperationException()
    }

    @Override VcsProject.CloneResult cloneToLocal() {
        throw new UnsupportedOperationException()
    }
}

