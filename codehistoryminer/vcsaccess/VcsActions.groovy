package codehistoryminer.vcsaccess
import codehistoryminer.core.common.langutil.Cancelled
import codehistoryminer.core.common.langutil.DateRange
import codehistoryminer.core.common.langutil.Measure
import codehistoryminer.core.vcs.miner.*
import codehistoryminer.core.vcs.miner.filetype.FileTypes
import codehistoryminer.core.vcs.miner.todo.TodoCountMiner
import codehistoryminer.core.vcs.reader.CommitProgressIndicator
import codehistoryminer.vcsaccess.implementation.wrappers.VcsProjectWrapper
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.update.UpdatedFilesListener
import com.intellij.util.messages.MessageBusConnection
import liveplugin.PluginUtil
import org.jetbrains.annotations.Nullable
import vcsreader.Change
import vcsreader.vcs.commandlistener.VcsCommand

import static codehistoryminer.core.common.langutil.Misc.withDefault
import static com.intellij.openapi.vcs.update.UpdatedFilesListener.UPDATED_FILES
import static com.intellij.openapi.vfs.VfsUtil.getCommonAncestor

class VcsActions {
	private final Measure measure
	private final VcsActionsLog log
	private final Map<String, MessageBusConnection> connectionByProjectName = [:]

    VcsActions(Measure measure = new Measure(), @Nullable VcsActionsLog log = null) {
		this.measure = measure
		this.log = log
	}

    Iterator<MinedCommit> readMinedCommits(List<DateRange> dateRanges, Project project, boolean grabChangeSizeInLines,
                                           ideIndicator, Cancelled cancelled) {
	    def fileTypes = new FileTypes([]) {
            @Override boolean isBinary(String fileName) {
                FileTypeManager.instance.getFileTypeByFileName(fileName).binary
            }
        }
        def noContentListener = new MinerListener() {
            @Override void failedToMine(Change change, String message, Throwable throwable) {
                log.failedToMine(message + ": " + change.toString() + ". " + throwable?.message)
            }
        }
        def miners = grabChangeSizeInLines ?
                [new FileChangeEventMiner(), new LineAndCharChangeMiner(fileTypes, noContentListener), new TodoCountMiner(fileTypes)] :
                [new FileChangeEventMiner()]
        def vcsProject = new VcsProjectWrapper(project, vcsRootsIn(project), commonVcsRootsAncestor(project), log)

	    def listener = new MiningMachine.Listener() {
		    @Override void onUpdate(CommitProgressIndicator indicator) { ideIndicator?.fraction = indicator.fraction() }
		    @Override void beforeCommand(VcsCommand command) {}
		    @Override void afterCommand(VcsCommand command) {}
		    @Override void onVcsError(String error) { log.errorReadingCommits(error) }
		    @Override void onException(Exception e) { log.errorReadingCommits(e.message) }
		    @Override void failedToMine(Change change, String description, Throwable throwable) {
			    log.onExtractChangeEventException(throwable)
		    }
	    }

	    def config = new MiningMachine.Config(miners, fileTypes, TimeZone.getDefault()).withListener(listener).withCacheFileContent(false)
	    def miningMachine = new MiningMachine(config)
	    miningMachine.mine(vcsProject, dateRanges, cancelled)
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
