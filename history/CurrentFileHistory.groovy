package history

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.AbstractVcs
import com.intellij.openapi.vcs.FilePathImpl
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import intellijeval.PluginUtil


class CurrentFileHistory {

	static showChangeEventsForCurrentFileHistory(Project project) {
		def file = PluginUtil.currentFileIn(project)
		def (errorMessage, List<VcsFileRevision> revisions) = tryToGetHistoryFor(file, project)
		if (errorMessage != null) {
			PluginUtil.show(errorMessage)
			return
		}
		PluginUtil.show("good to go")

		def changeEvents = extractChangeEvents(file, revisions, project)
		PluginUtil.showInConsole(toCsv(changeEvents.take(12)), "output", project)

		PluginUtil.show("done")
	}

	private static tryToGetHistoryFor(VirtualFile file, Project project) {
		if (file == null) return ["Virtual file was null"]

		AbstractVcs activeVcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file)
		if (activeVcs == null) return ["There is no history for '${file.name}'"]

		def historySession = activeVcs.vcsHistoryProvider.createSessionFor(new FilePathImpl(file))
		def revisions = historySession.revisionList.sort{ it.revisionDate }
		if (revisions.size() < 1) return ["There are no committed revisions for '${file.name}'"]

		def noErrors = null
		[noErrors, revisions]
	}

	private static List<Events.ChangeEvent> extractChangeEvents(VirtualFile file, List<VcsFileRevision> revisions, Project project) {
		def revisionPairs = [[null, revisions.first()]] + (0..<revisions.size() - 1).collect { revisions[it, it + 1] }
		def psiFileFactory = PsiFileFactory.getInstance(project)
		def parseAsPsi = { String text -> psiFileFactory.createFileFromText(file.name, file.fileType, text) }

		(List<Events.ChangeEvent>) revisionPairs.collectMany { VcsFileRevision before, VcsFileRevision after ->
			def beforeText = (before == null ? "" : new String(before.content))
			def afterText = new String(after.content)
			def commitInfo = new Events.CommitInfo(after.revisionNumber.asString(), after.author, after.revisionDate, after.commitMessage)
			ChangeExtractor.elementChangesBetween(beforeText, afterText, commitInfo, parseAsPsi)
		}
	}
}
