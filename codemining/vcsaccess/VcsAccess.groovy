package codemining.vcsaccess

import codemining.core.common.langutil.DateRange
import codemining.core.common.langutil.Measure
import codemining.core.vcs.*
import codemining.core.vcs.filetype.FileTypes
import codemining.vcsaccess.implementation.wrappers.VcsProjectWrapper
import com.intellij.openapi.fileTypes.FileTypeManager
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
import vcsreader.Change
import vcsreader.Commit

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

    Iterator<MungedCommit> readMungedCommits(DateRange dateRange, Project project, boolean grabChangeSizeInLines,
                                             VcsAccessReadListener readListener = null) {
        def fileTypes = new FileTypes([]) {
            @Override boolean isBinary(String fileName) {
                FileTypeManager.instance.getFileTypeByFileName(fileName).binary
            }
        }
        def mungerListener = new NoFileContentListener() {
            @Override void failedToLoadContent(Change change) {
                log.failedToLoadContent(change.toString())
            }
        }
        def mungers = grabChangeSizeInLines ?
                [new CommitMunger(), new LineAndCharChangeMunger(fileTypes, mungerListener)] :
                [new CommitMunger()]
        def projectWrapper = new VcsProjectWrapper(project, vcsRootsIn(project), commonVcsRootsAncestor(project), log)

        def listener = new MungingCommitReaderListener() {
            @Override def errorReadingCommits(String error) { log.errorReadingCommits(error) }
            @Override def onExtractChangeEventException(Exception e) { log.onExtractChangeEventException(e) }
            @Override def beforeMungingCommit(Commit commit) { readListener?.beforeMungingCommit(commit) }
            @Override def afterMungingCommit(Commit commit) { readListener?.afterMungingCommit(commit) }
        }

        new MungingCommitReader(projectWrapper, mungers, CommitReader.Config.defaults, listener).readCommits(dateRange)
    }

    // TODO  remove
    ChangeEventsReader changeEventsReaderFor(Project project, boolean grabChangeSizeInLines) {
        def fileTypes = new FileTypes([]) {
            @Override boolean isBinary(String fileName) {
                FileTypeManager.instance.getFileTypeByFileName(fileName).binary
            }
        }
        def listener = new NoFileContentListener() {
            @Override void failedToLoadContent(Change change) {
                log.failedToLoadContent(change.toString())
            }
        }
        def mungers = grabChangeSizeInLines ? [new LineAndCharChangeMunger(fileTypes, listener)] : []
        def commitMunger = new CommitMunger(mungers)
        def projectWrapper = new VcsProjectWrapper(project, vcsRootsIn(project), commonVcsRootsAncestor(project), log)
        new ChangeEventsReader(projectWrapper, commitMunger, log)
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

interface VcsAccessReadListener {
    def beforeMungingCommit(Commit commit)
    def afterMungingCommit(Commit commit)
}

interface VcsAccessLog {
	def errorReadingCommits(Exception e, Date fromDate, Date toDate)
    def errorReadingCommits(String error)
	def failedToLocate(VcsRoot vcsRoot, Project project)
    def onExtractChangeEventException(Exception e)
    def failedToLoadContent(String message)
}
