package vcsaccess.implementation.wrappers

import com.intellij.openapi.project.Project as IJProject
import com.intellij.openapi.vcs.changes.ChangeList as IJChangeList
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as IJCommit
import vcsaccess.implementation.CommitReader
import vcsreader.Change
import vcsreader.Commit
import vcsreader.VcsProject
import vcsreader.VcsRoot

class VcsRootWrapper implements VcsRoot {
    private final IJProject project
    private final com.intellij.openapi.vcs.VcsRoot vcsRoot
    private final int vcsRequestBatchSizeInDays = 10000 // TODO remove

    VcsRootWrapper(IJProject project, com.intellij.openapi.vcs.VcsRoot vcsRoot) {
        this.project = project
        this.vcsRoot = vcsRoot
    }

    @Override VcsProject.LogResult log(Date fromDate, Date toDate) {
        def reader = new CommitReader(project, vcsRequestBatchSizeInDays)
        def commits = reader.readCommits(fromDate, toDate, true, [vcsRoot])

        def result = []
        for (IJCommit ijCommit in commits) {
            if (ijCommit == CommitReader.NO_MORE_COMMITS) break

            def revision = ijCommit.changes.first().afterRevision.revisionNumber.asString()
            def revisionBefore = ijCommit.changes.first().beforeRevision.revisionNumber.asString()

            def commit = new Commit(
                    revision,
                    revisionBefore,
                    ijCommit.commitDate,
                    ijCommit.committerName,
                    ijCommit.comment,
                    convertChangesFrom(ijCommit)
            )

            result << commit
        }
        new VcsProject.LogResult(result, [])
    }

    private static List<Change> convertChangesFrom(IJChangeList changeList) {
        changeList.changes.collect { new ChangeWrapper(it) }
    }

    @Override VcsProject.LogContentResult contentOf(String filePath, String revision) {
        throw new IllegalStateException("Shouldn't be called (filePath: ${filePath}; revision: ${revision}")
    }

    @Override VcsProject.UpdateResult update() {
        throw new UnsupportedOperationException()
    }

    @Override VcsProject.CloneResult cloneToLocal() {
        throw new UnsupportedOperationException()
    }
}

