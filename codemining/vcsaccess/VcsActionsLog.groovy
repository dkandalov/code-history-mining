package codemining.vcsaccess

import codemining.core.common.langutil.Date2
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot

interface VcsActionsLog {
    def errorReadingCommits(Exception e, Date2 fromDate, Date2 toDate)
    def errorReadingCommits(String error)
    def failedToLocate(VcsRoot vcsRoot, Project project)
    def onExtractChangeEventException(Exception e)
    def failedToLoadContent(String message)
}
