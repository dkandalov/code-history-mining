package codemining.vcsaccess

import vcsreader.Commit

interface VcsActionsReadListener {
    def beforeMungingCommit(Commit commit)
    def afterMungingCommit(Commit commit)
}
