package codehistoryminer.plugin.vcsaccess.implementation.wrappers
import com.intellij.openapi.project.Project as IJProject
import com.intellij.openapi.vcs.VcsRoot as IJVcsRoot
import codehistoryminer.plugin.vcsaccess.VcsActionsLog
import org.vcsreader.VcsProject
import org.vcsreader.VcsRoot

class VcsProjectWrapper extends VcsProject {
    VcsProjectWrapper(IJProject project, List<IJVcsRoot> roots, String commonVcsRoot, VcsActionsLog log) {
        super(convertRoots(project, roots, commonVcsRoot, log))
    }

    private static List<VcsRoot> convertRoots(IJProject project, List<IJVcsRoot> vcsRoots, String commonVcsRoot, VcsActionsLog log) {
        vcsRoots.collect { new VcsRootWrapper(project, it, commonVcsRoot, log) }
    }
}
