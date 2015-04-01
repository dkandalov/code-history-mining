package codemining.plugin

import codemining.core.common.langutil.Date2
import codemining.core.common.langutil.DateRange
import codemining.core.common.langutil.Measure
import codemining.core.historystorage.EventStorage
import codemining.historystorage.HistoryGrabberConfig
import codemining.historystorage.HistoryStorage
import codemining.plugin.ui.UI
import codemining.vcsaccess.VcsActions
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.junit.Test

import static codemining.core.common.langutil.DateTimeUtil.*
import static codemining.plugin.GroovyStubber.*

class CodeMiningPluginTest {

	@Test def "on VCS update does nothing if already grabbed on this date"() {
		// given
		def grabbedVcs = false
		def historyStorage = stub(HistoryStorage, [
				eventStorageFor: returns(stub(EventStorage, [
						storedDateRange: returns(dateRange("01/11/2012", "20/11/2012"))
				])),
				loadGrabberConfigFor: returns(someConfig.withLastGrabTime(dateTime("09:00 23/11/2012")))
		])
		def vcsAccess = stub(VcsActions,
				[readMinedCommits: { DateRange dateRange, Project project, boolean grabChangeSizeInLines, readListener ->
					grabbedVcs = true
					[].iterator()
				}])

		def ui = stub(UI, [runInBackground: runOnTheSameThread])
		def miner = new CodeMiningPlugin(ui, historyStorage, vcsAccess, new Measure())

		// when / then
		def now = dateTime("23/11/2012", TimeZone.default)
		miner.grabHistoryOnVcsUpdate(someProject, now)
		assert !grabbedVcs
	}

	@Test def "on VCS update grabs history from the today to the latest event in file history"() {
		// given
		Date2 grabbedFrom = null
		Date2 grabbedTo = null
		def historyStorage = stub(HistoryStorage, [
				eventStorageFor: returns(stub(EventStorage, [
                        storedDateRange: returns(dateRange("01/11/2012", "20/11/2012"))
				])),
				loadGrabberConfigFor: returns(someConfig.withLastGrabTime(dateTime("13:40 20/11/2012")))
		])
		def vcsAccess = stub(VcsActions,
				[readMinedCommits: { DateRange dateRange, Project project, boolean grabChangeSizeInLines, readListener ->
					grabbedFrom = dateRange.from
					grabbedTo = dateRange.to
					[].iterator()
				}])
		def ui = stub(UI, [runInBackground: runOnTheSameThread])
		def miner = new CodeMiningPlugin(ui, historyStorage, vcsAccess, new Measure())

		// when
		def now = dateTime("23/11/2012")
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
					onOkCallback(new HistoryGrabberConfig(Date2.today().shiftDays(-300), Date2.today(), "some.csv", false, grabOnVcsUpdate, new Date(0)))
				}
		])
		def vcsAccess = stub(VcsActions, [
				readMinedCommits: returns([].iterator()),
				addVcsUpdateListenerFor: { String projectName, listener -> listeningToProject = projectName }
		])
		def miner = new CodeMiningPlugin(ui, stub(HistoryStorage), vcsAccess, new Measure())

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
		def miner = new CodeMiningPlugin(ui, stub(HistoryStorage), vcsAccess, new Measure())

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
