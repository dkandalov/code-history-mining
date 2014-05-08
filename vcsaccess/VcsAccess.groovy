package vcsaccess
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.update.UpdatedFilesListener
import com.intellij.util.messages.MessageBusConnection
import common.langutil.Measure
import liveplugin.PluginUtil
import org.jetbrains.annotations.Nullable
import vcsaccess.implementation.CommitFilesMunger
import vcsaccess.implementation.CommitReader

import static com.intellij.openapi.vcs.update.UpdatedFilesListener.UPDATED_FILES
import static com.intellij.openapi.vfs.VfsUtil.getCommonAncestor
import static vcsaccess.implementation.CommitMungingUtil.withDefault

class VcsAccess {
	private final Measure measure
	private final VcsAccessLog log
	private final Map<String, MessageBusConnection> connectionByProjectName = [:]

	VcsAccess(Measure measure = new Measure(), @Nullable VcsAccessLog log = null) {
		this.measure = measure
		this.log = log
	}

    ChangeEventsReader changeEventsReaderFor(Project project, boolean grabChangeSizeInLines) {
		def vcsRequestBatchSizeInDays = 1 // based on personal observation (hardcoded so that not to clutter UI dialog)
        new ChangeEventsReader(
				vcsRootsIn(project),
				new CommitReader(project, vcsRequestBatchSizeInDays, measure, log),
				new CommitFilesMunger(commonVcsRootsAncestor(project), grabChangeSizeInLines).&mungeCommit
		)
	}

    def addVcsUpdateListenerFor(String projectName, Closure closure) {
		if (connectionByProjectName.containsKey(projectName)) return

		Project project = ProjectManager.instance.openProjects.find{ it.name == projectName }
		if (project == null) return

		def connection = project.messageBus.connect(project)
		connection.subscribe(UPDATED_FILES, new UpdatedFilesListener() {
			@Override void consume(Set<String> files) {
				PluginUtil.invokeLaterOnEDT{
					closure.call(project)
				}
			}
		})
		connectionByProjectName.put(projectName, connection)
	}

	def removeVcsUpdateListenerFor(String projectName) {
		def connection = connectionByProjectName.get(projectName)
		if (connection == null) return
		connection.disconnect()
	}

    @SuppressWarnings("GrMethodMayBeStatic")
    def dispose(oldVcsAccess) {
		oldVcsAccess.connectionByProjectName.values().each {
			it.disconnect()
		}
	}

    @SuppressWarnings("GrMethodMayBeStatic")
    boolean noVCSRootsIn(Project project) {
        vcsRootsIn(project).size() == 0
    }

    static List<VcsRoot> vcsRootsIn(Project project) {
        ProjectLevelVcsManager.getInstance(project).allVcsRoots
    }

    static String commonVcsRootsAncestor(Project project) {
        withDefault("", getCommonAncestor(vcsRootsIn(project).collect { it.path })?.canonicalPath)
    }
}

interface VcsAccessLog {
	def errorReadingCommits(Exception e, Date fromDate, Date toDate)

	def failedToLocate(VcsRoot vcsRoot, Project project)
}
