package codehistoryminer.vcsaccess

import codehistoryminer.core.common.langutil.Date
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot

interface VcsActionsLog {
    def errorReadingCommits(Exception e, Date fromDate, Date toDate)
    def errorReadingCommits(String error)
    def failedToLocate(VcsRoot vcsRoot, Project project)
    def onExtractChangeEventException(Exception e)
    def failedToMine(String message)
}
