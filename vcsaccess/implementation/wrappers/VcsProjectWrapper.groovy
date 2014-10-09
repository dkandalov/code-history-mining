package vcsaccess.implementation.wrappers
import com.intellij.openapi.project.Project as IJProject
import com.intellij.openapi.vcs.VcsRoot as IJVcsRoot
import vcsaccess.VcsAccessLog
import vcsreader.VcsProject
import vcsreader.VcsRoot

class VcsProjectWrapper extends VcsProject {
    VcsProjectWrapper(IJProject project, List<IJVcsRoot> roots, String commonVcsRoot, VcsAccessLog log) {
        super(convertRoots(project, roots, commonVcsRoot, log))
    }

    private static List<VcsRoot> convertRoots(IJProject project, List<IJVcsRoot> vcsRoots, String commonVcsRoot, VcsAccessLog log) {
        vcsRoots.collect { new VcsRootWrapper(project, it, commonVcsRoot, log) }
    }
}
