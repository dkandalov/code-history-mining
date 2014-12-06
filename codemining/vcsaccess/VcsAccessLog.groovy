package codemining.vcsaccess

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot

interface VcsAccessLog {
    def errorReadingCommits(Exception e, Date fromDate, Date toDate)
    def errorReadingCommits(String error)
    def failedToLocate(VcsRoot vcsRoot, Project project)
    def onExtractChangeEventException(Exception e)
    def failedToLoadContent(String message)
}
