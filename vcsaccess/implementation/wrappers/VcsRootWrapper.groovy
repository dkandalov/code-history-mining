package vcsaccess.implementation.wrappers

import com.intellij.openapi.project.Project as IJProject
import com.intellij.openapi.vcs.VcsRoot as IJVcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as IJCommit
import vcsaccess.implementation.CommitReader
import vcsreader.Change
import vcsreader.Commit
import vcsreader.VcsProject
import vcsreader.VcsRoot

import static codemining.core.common.langutil.Misc.withDefault
import static vcsreader.Change.noRevision

class VcsRootWrapper implements VcsRoot {
    private final IJProject project
    private final IJVcsRoot vcsRoot
    private final String commonVcsRoot
    private final int vcsRequestBatchSizeInDays = 10000 // TODO remove

    VcsRootWrapper(IJProject project, IJVcsRoot vcsRoot, String commonVcsRoot) {
        this.project = project
        this.vcsRoot = vcsRoot
        this.commonVcsRoot = commonVcsRoot
    }

    @Override VcsProject.LogResult log(Date fromDate, Date toDate) {
        def reader = new CommitReader(project, vcsRequestBatchSizeInDays)
        def commits = reader.readCommits(fromDate, toDate, true, [vcsRoot]).toList().findAll{ it != null }

        def result = []
        for (IJCommit ijCommit in commits) {
            if (ijCommit == CommitReader.NO_MORE_COMMITS) break

            def revision = withDefault(noRevision, ijCommit.changes.first().afterRevision?.revisionNumber?.asString())
            def revisionBefore = withDefault(noRevision, ijCommit.changes.first().beforeRevision?.revisionNumber?.asString())

            def changes = convertChangesFrom(ijCommit)
            if (changes.empty) continue

            def commit = new Commit(
                    revision,
                    revisionBefore,
                    ijCommit.commitDate,
                    ijCommit.committerName,
                    ijCommit.comment.trim(),
                    changes
            )

            result.add(commit)
        }

        new VcsProject.LogResult(result, [])
    }

    private List<Change> convertChangesFrom(IJCommit ijCommit) {
        ijCommit.changes
            .collect { ChangeWrapper.create(it, commonVcsRoot) }
            .findAll{ it != ChangeWrapper.none }
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

