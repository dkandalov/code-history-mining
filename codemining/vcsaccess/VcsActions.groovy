package codemining.vcsaccess

import codemining.core.common.langutil.Cancelled
import codemining.core.common.langutil.DateRange
import codemining.core.common.langutil.Measure
import codemining.core.common.langutil.Progress
import codemining.core.vcs.*
import codemining.core.vcs.filetype.FileTypes
import codemining.core.vcs.todo.TodoCountMiner
import codemining.vcsaccess.implementation.wrappers.VcsProjectWrapper
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
import vcsreader.Commit

import static codemining.core.common.langutil.Misc.withDefault
import static codemining.core.vcs.CommitReaderConfig.noCachingDefaults
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

    Iterator<MinedCommit> readMinedCommits(DateRange dateRange, Project project, boolean grabChangeSizeInLines,
                                           Progress progress, Cancelled cancelled) {
	    def fileTypes = new FileTypes([]) {
            @Override boolean isBinary(String fileName) {
                FileTypeManager.instance.getFileTypeByFileName(fileName).binary
            }
        }
        def noContentListener = new NoFileContentListener() {
            @Override void failedToLoadContent(Change change) {
                log.failedToLoadContent(change.toString())
            }
        }
        def miners = grabChangeSizeInLines ?
                [new MainFileMiner(), new LineAndCharChangeMiner(fileTypes, noContentListener), new TodoCountMiner(fileTypes)] :
                [new MainFileMiner()]
        def projectWrapper = new VcsProjectWrapper(project, vcsRootsIn(project), commonVcsRootsAncestor(project), log)

        def listener = new MiningCommitReaderListener() {
            @Override void errorReadingCommits(String error) { log.errorReadingCommits(error) }
            @Override void onExtractChangeEventException(Exception e) { log.onExtractChangeEventException(e) }
            @Override void onCurrentDateRange(DateRange range) { progress.add(range.durationInDays()) }
            @Override void beforeMiningCommit(Commit commit) {}
            @Override void afterMiningCommit(Commit commit) {}
        }

	    def commitReader = new MiningCommitReader(new CommitReader(projectWrapper, noCachingDefaults), miners, listener)
	    cancelled.wrap(commitReader.readCommits(dateRange))
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
