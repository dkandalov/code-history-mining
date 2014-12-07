package codemining.vcsaccess.implementation.wrappers
import com.intellij.openapi.project.Project as IJProject
import com.intellij.openapi.vcs.VcsRoot as IJVcsRoot
import codemining.vcsaccess.VcsActionsLog
import vcsreader.VcsProject
import vcsreader.VcsRoot

class VcsProjectWrapper extends VcsProject {
    VcsProjectWrapper(IJProject project, List<IJVcsRoot> roots, String commonVcsRoot, VcsActionsLog log) {
        super(convertRoots(project, roots, commonVcsRoot, log))
    }

    private static List<VcsRoot> convertRoots(IJProject project, List<IJVcsRoot> vcsRoots, String commonVcsRoot, VcsActionsLog log) {
        vcsRoots.collect { new VcsRootWrapper(project, it, commonVcsRoot, log) }
    }
}
