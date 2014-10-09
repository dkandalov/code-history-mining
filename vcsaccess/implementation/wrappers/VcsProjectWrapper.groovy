package vcsaccess.implementation.wrappers
import com.intellij.openapi.project.Project as IJProject
import com.intellij.openapi.vcs.VcsRoot as IJVcsRoot
import vcsaccess.VcsAccessLog
import vcsreader.VcsProject
import vcsreader.VcsRoot

class VcsProjectWrapper extends VcsProject {
    private final IJProject project
    private final VcsAccessLog log

    VcsProjectWrapper(IJProject project, List<IJVcsRoot> roots, String commonVcsRoot, VcsAccessLog log) {
        super(convert(roots, project, commonVcsRoot))
        this.project = project
        this.log = log
    }

    private static List<VcsRoot> convert(Collection<IJVcsRoot> vcsRoots, IJProject project, String commonVcsRoot) {
        vcsRoots.collect { new VcsRootWrapper(project, it, commonVcsRoot, log) }
    }
}
