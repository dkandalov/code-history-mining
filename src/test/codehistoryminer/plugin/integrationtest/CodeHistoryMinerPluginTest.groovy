package codehistoryminer.plugin.integrationtest

import codehistoryminer.core.historystorage.EventStorageReader
import codehistoryminer.core.historystorage.EventStorageWriter
import codehistoryminer.core.lang.DateRange
import codehistoryminer.core.miner.filechange.CommitInfo
import codehistoryminer.core.miner.filechange.FileChangeInfo
import codehistoryminer.plugin.CodeHistoryMinerPlugin
import codehistoryminer.plugin.historystorage.HistoryGrabberConfig
import codehistoryminer.plugin.historystorage.HistoryStorage
import codehistoryminer.plugin.historystorage.ScriptStorage
import codehistoryminer.plugin.ui.UI
import codehistoryminer.plugin.vcsaccess.VcsActions
import codehistoryminer.publicapi.analysis.filechange.FileChange
import codehistoryminer.publicapi.lang.Cancelled
import codehistoryminer.publicapi.lang.Date
import codehistoryminer.publicapi.lang.Time
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.junit.Test

import static codehistoryminer.core.lang.DateTimeTestUtil.*
import static codehistoryminer.plugin.integrationtest.GroovyStubber.*
import static codehistoryminer.publicapi.analysis.filechange.ChangeType.MODIFIED

class CodeHistoryMinerPluginTest {

	@Test void "on VCS update does nothing if already grabbed on this date"() {
		// given
		def grabbedVcs = false
		def historyStorage = stub(HistoryStorage, [
				eventStorageWriter: returns(stub(EventStorageWriter, [:])),
				loadGrabberConfigFor: returns(someConfig.withLastGrabTime(time("09:00 23/11/2012")))
		])
		def vcsAccess = stub(VcsActions,
				[readMinedCommits: { List<DateRange> dateRanges, Project project, boolean grabChangeSizeInLines, readListener, Cancelled cancelled ->
					grabbedVcs = true
					[].iterator()
				}])

		def ui = stub(UI, [runInBackground: runOnTheSameThread])
		def miner = new CodeHistoryMinerPlugin(ui, historyStorage, stub(ScriptStorage), vcsAccess)

		// when / then
		def now = time("23/11/2012", TimeZone.default)
		miner.grabHistoryOnVcsUpdate(someProject, now)
		assert !grabbedVcs
	}

	@Test void "on VCS update grabs history from today to the latest event in file history"() {
		// given
		List<DateRange> grabbedDateRanges = null
		def historyStorage = stub(HistoryStorage, [
				eventStorageReader: returns(stub(EventStorageReader, [
                        firstEvent: returns(eventWithCommitDate("01/11/2012")),
                        lastEvent: returns(eventWithCommitDate("20/11/2012"))
				])),
				eventStorageWriter: returns(stub(EventStorageWriter, [:])),
				loadGrabberConfigFor: returns(someConfig.withLastGrabTime(time("13:40 20/11/2012")))
		])
		def vcsAccess = stub(VcsActions,
				[readMinedCommits: { List<DateRange> dateRanges, Project project, boolean grabChangeSizeInLines, progress, cancelled ->
					grabbedDateRanges = dateRanges
					[].iterator()
				}])
		def ui = stub(UI, [runInBackground: runOnTheSameThread])
		def miner = new CodeHistoryMinerPlugin(ui, historyStorage, stub(ScriptStorage), vcsAccess)

		// when
		def now = time("23/11/2012")
        miner.grabHistoryOnVcsUpdate(someProject, now)

        // then
        assert grabbedDateRanges == [new DateRange(date("20/11/2012"), date("23/11/2012"))]
    }

	@Test void "on grab history should register VCS update listener"() {
		// given
		def listeningToProject = ""
		def ui = stub(UI, [
				showGrabbingDialog: { config, project, onApplyConfig, Closure onOkCallback ->
					def grabOnVcsUpdate = true
					onOkCallback(new HistoryGrabberConfig(Date.today().shiftDays(-300), Date.today(), "some.csv", false, grabOnVcsUpdate, Time.zero()))
				}
		])
		def vcsAccess = stub(VcsActions, [
				readMinedCommits: returns([].iterator()),
				addVcsUpdateListenerFor: { String projectName, listener -> listeningToProject = projectName }
		])
		def miner = new CodeHistoryMinerPlugin(ui, stub(HistoryStorage), stub(ScriptStorage), vcsAccess)

		// when / then
		miner.grabHistoryOf(someProject)
		assert listeningToProject == someProject.name
	}

	@Test void "should only grab history of one project at a time"() {
		// given
		def showedGrabberDialog = 0
		def showedGrabbingInProgress = 0
		def ui = stub(UI, [
				showGrabbingDialog: { config, project, onApplyConfig, Closure onOkCallback ->
					showedGrabberDialog++
					onOkCallback(someConfig)
				},
				showGrabbingInProgressMessage: does{ showedGrabbingInProgress++ },
		])
		def vcsAccess = stub(VcsActions, [readMinedCommits: returns([].iterator())])
		def miner = new CodeHistoryMinerPlugin(ui, stub(HistoryStorage), stub(ScriptStorage), vcsAccess)

		// when / then
		miner.grabHistoryOf(someProject)
		assert showedGrabberDialog == 1
		assert showedGrabbingInProgress == 0

		miner.grabHistoryOf(someProject)
		assert showedGrabberDialog == 1
		assert showedGrabbingInProgress == 1
	}

	private static eventWithCommitDate(String date) {
		def commitInfo = new CommitInfo("43b0fe352d5bced0c341640d0c630d23f2022a7e", "dsaff <dsaff>", time("14:42:16 ${date}"), "")
		def fileChangeInfo = new FileChangeInfo("", "Theories.java", "", "/src/org/junit/experimental/theories", MODIFIED)
		new FileChange(commitInfo, fileChangeInfo)
	}

	private static final runOnTheSameThread = { taskDescription, closure -> closure([:] as ProgressIndicator) }
	private static final someProject = stub(Project, [getName: returns("someProject")])
	private static final someConfig = HistoryGrabberConfig.defaultConfig()
}
