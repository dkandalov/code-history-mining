package vcsaccess.implementation.wrappers
import com.intellij.openapi.project.Project as IJProject
import com.intellij.openapi.vcs.VcsRoot as IJVcsRoot
import vcsreader.VcsProject
import vcsreader.VcsRoot

class VcsProjectWrapper extends VcsProject {
    private final IJProject project

    VcsProjectWrapper(IJProject project, List<IJVcsRoot> roots) {
        super(convert(roots))
        this.project = project
    }

    private static List<VcsRoot> convert(Collection<IJVcsRoot> vcsRoots) {
        vcsRoots.collect { new VcsRootWrapper(project, it) }
    }
}
