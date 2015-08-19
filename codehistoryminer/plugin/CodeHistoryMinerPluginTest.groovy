package codehistoryminer.plugin
import codehistoryminer.core.common.langutil.Date
import codehistoryminer.core.common.langutil.DateRange
import codehistoryminer.core.common.langutil.Measure
import codehistoryminer.core.common.langutil.Time
import codehistoryminer.core.historystorage.EventStorage
import codehistoryminer.historystorage.HistoryGrabberConfig
import codehistoryminer.historystorage.HistoryStorage
import codehistoryminer.plugin.ui.UI
import codehistoryminer.vcsaccess.VcsActions
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.junit.Test

import static codehistoryminer.core.common.langutil.DateTimeTestUtil.*
import static codehistoryminer.plugin.GroovyStubber.*

class CodeHistoryMinerPluginTest {

	@Test def "on VCS update does nothing if already grabbed on this date"() {
		// given
		def grabbedVcs = false
		def historyStorage = stub(HistoryStorage, [
				eventStorageFor: returns(stub(EventStorage, [
						storedDateRange: returns(dateRange("01/11/2012", "20/11/2012"))
				])),
				loadGrabberConfigFor: returns(someConfig.withLastGrabTime(time("09:00 23/11/2012")))
		])
		def vcsAccess = stub(VcsActions,
				[readMinedCommits: { DateRange dateRange, Project project, boolean grabChangeSizeInLines, readListener ->
					grabbedVcs = true
					[].iterator()
				}])

		def ui = stub(UI, [runInBackground: runOnTheSameThread])
		def miner = new CodeHistoryMinerPlugin(ui, historyStorage, vcsAccess, new Measure())

		// when / then
		def now = time("23/11/2012", TimeZone.default)
		miner.grabHistoryOnVcsUpdate(someProject, now)
		assert !grabbedVcs
	}

	@Test def "on VCS update grabs history from today to the latest event in file history"() {
		// given
		Date grabbedFrom = null
		Date grabbedTo = null
		def historyStorage = stub(HistoryStorage, [
				eventStorageFor: returns(stub(EventStorage, [
                        storedDateRange: returns(dateRange("01/11/2012", "20/11/2012"))
				])),
				loadGrabberConfigFor: returns(someConfig.withLastGrabTime(time("13:40 20/11/2012")))
		])
		def vcsAccess = stub(VcsActions,
				[readMinedCommits: { DateRange dateRange, Project project, boolean grabChangeSizeInLines, progress, cancelled ->
					grabbedFrom = dateRange.from
					grabbedTo = dateRange.to
					[].iterator()
				}])
		def ui = stub(UI, [runInBackground: runOnTheSameThread])
		def miner = new CodeHistoryMinerPlugin(ui, historyStorage, vcsAccess, new Measure())

		// when
		def now = time("23/11/2012")
        miner.grabHistoryOnVcsUpdate(someProject, now)

        // then
        assert grabbedFrom == date("20/11/2012")
        assert grabbedTo == date("23/11/2012")
    }

	@Test def "on grab history should register VCS update listener"() {
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
		def miner = new CodeHistoryMinerPlugin(ui, stub(HistoryStorage), vcsAccess, new Measure())

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
		def miner = new CodeHistoryMinerPlugin(ui, stub(HistoryStorage), vcsAccess, new Measure())

		// when / then
		miner.grabHistoryOf(someProject)
		assert showedGrabberDialog == 1
		assert showedGrabbingInProgress == 0

		miner.grabHistoryOf(someProject)
		assert showedGrabberDialog == 1
		assert showedGrabbingInProgress == 1
	}

	private static final runOnTheSameThread = { taskDescription, closure -> closure([:] as ProgressIndicator) }
	private static final someProject = stub(Project, [getName: returns("someProject")])
	private static final someConfig = HistoryGrabberConfig.defaultConfig()
}
