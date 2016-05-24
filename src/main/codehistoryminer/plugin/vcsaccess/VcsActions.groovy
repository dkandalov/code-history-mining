package codehistoryminer.plugin.vcsaccess

import codehistoryminer.core.lang.DateRange
import codehistoryminer.core.miner.MinedCommit
import codehistoryminer.core.miner.MinerListener
import codehistoryminer.core.miner.MiningMachine
import codehistoryminer.core.miner.filechange.FileChangeMiner
import codehistoryminer.core.miner.linchangecount.LineAndCharChangeMiner
import codehistoryminer.core.miner.todo.TodoCountMiner
import codehistoryminer.core.vcsreader.CommitProgressIndicator
import codehistoryminer.plugin.vcsaccess.implementation.IJFileTypes
import codehistoryminer.plugin.vcsaccess.implementation.wrappers.VcsProjectWrapper
import codehistoryminer.publicapi.lang.Cancelled
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.update.UpdatedFilesListener
import com.intellij.util.messages.MessageBusConnection
import liveplugin.PluginUtil
import org.jetbrains.annotations.Nullable
import org.vcsreader.VcsChange
import org.vcsreader.vcs.commandlistener.VcsCommand

import static codehistoryminer.core.lang.Misc.withDefault
import static com.intellij.openapi.vcs.update.UpdatedFilesListener.UPDATED_FILES
import static com.intellij.openapi.vfs.VfsUtil.getCommonAncestor

class VcsActions {
	private final VcsActionsLog log
	private final Map<String, MessageBusConnection> connectionByProjectName = [:]

    VcsActions(@Nullable VcsActionsLog log = null) {
		this.log = log
	}

    Iterator<MinedCommit> readMinedCommits(List<DateRange> dateRanges, Project project, boolean grabChangeSizeInLines,
                                                                       ideIndicator, Cancelled cancelled) {
	    def fileTypes = new IJFileTypes()
        def noContentListener = new MinerListener() {
            @Override void failedToMine(VcsChange change, String message, Throwable throwable) {
                log.failedToMine(message + ": " + change.toString() + ". " + throwable?.message)
            }
        }
        def miners = grabChangeSizeInLines ?
                [new FileChangeMiner(), new LineAndCharChangeMiner(fileTypes, noContentListener), new TodoCountMiner(fileTypes)] :
                [new FileChangeMiner()]
        def vcsProject = new VcsProjectWrapper(project, vcsRootsIn(project), commonVcsRootsAncestor(project), log)

	    def listener = new MiningMachine.Listener() {
		    @Override void onUpdate(CommitProgressIndicator indicator) { ideIndicator?.fraction = indicator.fraction() }
		    @Override void beforeCommand(VcsCommand command) {}
		    @Override void afterCommand(VcsCommand command) {}
		    @Override void onVcsError(String error) { log.errorReadingCommits(error) }
		    @Override void onException(Exception e) { log.errorReadingCommits(e.message) }
		    @Override void failedToMine(VcsChange change, String description, Throwable throwable) {
			    log.onFailedToMineException(throwable)
		    }
	    }

	    def config = new MiningMachine.Config(miners, fileTypes, TimeZone.getDefault())
			    .withListener(listener)
			    .withCacheFileContent(false)
	            .withVcsRequestSizeInDays(1)
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
