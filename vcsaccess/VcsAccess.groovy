package vcsaccess
import codemining.core.common.langutil.Measure
import codemining.core.vcs.CommitMunger
import codemining.core.vcs.CommitMungerListener
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.update.UpdatedFiles
import com.intellij.openapi.vcs.update.UpdatedFilesListener
import com.intellij.util.messages.MessageBusConnection
import liveplugin.PluginUtil
import org.jetbrains.annotations.Nullable
import vcsaccess.implementation.CommitFilesMunger
import vcsaccess.implementation.CommitReader
import vcsaccess.implementation.wrappers.VcsProjectWrapper
import vcsreader.Change

import static codemining.core.common.langutil.Misc.withDefault
import static com.intellij.openapi.vcs.VcsActiveEnvironmentsProxy.proxyVcs
import static com.intellij.openapi.vcs.update.UpdatedFilesListener.UPDATED_FILES
import static com.intellij.openapi.vfs.VfsUtil.getCommonAncestor

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
            new CommitFilesMunger(commonVcsRootsAncestor(project), grabChangeSizeInLines).&mungeCommit,
            log
		)
	}

    ChangeEventsReader2 changeEventsReader2For(Project project, boolean grabChangeSizeInLines) {
        def commitMunger = new CommitMunger(grabChangeSizeInLines, new CommitMungerListener() {
            @Override void failedToLoadContent(Change change) {
                log.failedToLoadContent(change)
            }
        })
        def projectWrapper = new VcsProjectWrapper(project, vcsRootsIn(project), commonVcsRootsAncestor(project))
        new ChangeEventsReader2(projectWrapper, commitMunger, log)
    }

    @SuppressWarnings("GrMethodMayBeStatic")
    def update(Project project) {
        def vcsManager = project.getComponent(ProjectLevelVcsManager)
        vcsManager.allActiveVcss.each { vcs ->
            def paths = vcsManager.allVcsRoots.collect { new FilePathImpl(it.path) }.toArray(new FilePath[0])
            def updatedFiles = UpdatedFiles.create()
            def context = new Ref(null)
            proxyVcs(vcs).updateEnvironment.updateDirectories(paths, updatedFiles, new EmptyProgressIndicator(), context)
        }
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

    def errorReadingCommits(String error)

	def failedToLocate(VcsRoot vcsRoot, Project project)

    def onExtractChangeEventException(Exception e)

    def failedToLoadContent(Change change)
}
