package codehistoryminer.plugin.vcsaccess

import codehistoryminer.publicapi.lang.Date
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot

interface VcsActionsLog {
    def errorReadingCommits(Exception e, Date fromDate, Date toDate)
    def errorReadingCommits(String error)
    def failedToLocate(VcsRoot vcsRoot, Project project)
    def onFailedToMineException(Throwable t)
    def failedToMine(String message)
}
