package codemining.vcsaccess

import vcsreader.Commit

interface VcsActionsReadListener {
    def beforeMiningCommit(Commit commit)
    def afterMiningCommit(Commit commit)
}
