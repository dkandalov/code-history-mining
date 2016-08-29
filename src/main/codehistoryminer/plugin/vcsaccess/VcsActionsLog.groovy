package codehistoryminer.plugin.vcsaccess

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsRoot
import org.vcsreader.lang.TimeRange

interface VcsActionsLog {
    def errorReadingCommits(Exception e, TimeRange timeRange)
    def errorReadingCommits(String error)
    def failedToLocate(VcsRoot vcsRoot, Project project)
    def onFailedToMineException(Throwable t)
    def failedToMine(String message)
}
