package codemining.vcsaccess

import vcsreader.Commit

interface VcsAccessReadListener {
    def beforeMungingCommit(Commit commit)
    def afterMungingCommit(Commit commit)
}
