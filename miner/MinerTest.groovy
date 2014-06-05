package miner

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import codemining.core.historystorage.EventStorage
import codemining.core.historystorage.HistoryStorage
import miner.ui.UI
import org.junit.Test
import codemining.core.common.langutil.Measure
import codemining.core.vcsaccess.ChangeEventsReader
import codemining.core.historystorage.HistoryGrabberConfig
import codemining.core.vcsaccess.VcsAccess

import static codemining.core.common.langutil.DateTimeUtil.date
import static codemining.core.common.langutil.DateTimeUtil.dateTime
import static miner.GroovyStubber.*

class MinerTest {

	@Test def "on VCS update does nothing if already grabbed on this date"() {
		// given
		def grabbedVcs = false
		def historyStorage = stub(HistoryStorage, [
				eventStorageFor: returns(stub(EventStorage, [
						getMostRecentEventTime: returns(dateTime("13:40 20/11/2012"))
				])),
				loadGrabberConfigFor: returns(someConfig.withLastGrabTime(dateTime("09:00 23/11/2012")))
		])
		def vcsAccess = stub(VcsAccess, [changeEventsReaderFor: returns(
				stub(ChangeEventsReader, [
					readPastToPresent: { Object... args ->
						grabbedVcs = true
					}
		]))])
		def ui = stub(UI, [runInBackground: runOnTheSameThread])
		def miner = new Miner(ui, historyStorage, vcsAccess, new Measure())

		// when / then
		def today = date("23/11/2012")
		miner.grabHistoryOnVcsUpdate(someProject, today)
		assert !grabbedVcs
	}

	@Test def "on VCS update grabs history from latest event in file history util today"() {
		// given
		Date grabbedFrom = null
		Date grabbedTo = null
		def historyStorage = stub(HistoryStorage, [
				eventStorageFor: returns(stub(EventStorage, [
						getMostRecentEventTime: returns(dateTime("13:40 20/11/2012"))
				])),
				loadGrabberConfigFor: returns(someConfig.withLastGrabTime(dateTime("13:40 20/11/2012")))
		])
		def vcsAccess = stub(VcsAccess, [changeEventsReaderFor: returns(
				stub(ChangeEventsReader, [
					readPastToPresent: { Date historyStart, Date historyEnd, isCancelled, consumeWrapper, consume ->
						grabbedFrom = historyStart
						grabbedTo = historyEnd
					}
		]))])
		def ui = stub(UI, [runInBackground: runOnTheSameThread])
		def miner = new Miner(ui, historyStorage, vcsAccess, new Measure())

		// when / then
		def today = date("23/11/2012")
		miner.grabHistoryOnVcsUpdate(someProject, today)
		assert grabbedFrom == dateTime("13:40 20/11/2012")
		assert grabbedTo == dateTime("00:00 23/11/2012")
	}

	@Test def "on grab history should register VCS update listener"() {
		// given
		def listeningToProject = ""
		def ui = stub(UI, [
				showGrabbingDialog: { config, project, onApplyConfig, Closure onOkCallback ->
					def grabOnVcsUpdate = true
					onOkCallback(new HistoryGrabberConfig(new Date() - 300, new Date(), "some.csv", false, grabOnVcsUpdate, new Date(0)))
				}
		])
		def vcsAccess = stub(VcsAccess, [
				changeEventsReaderFor: returns(stub(ChangeEventsReader)),
				addVcsUpdateListenerFor: { String projectName, listener -> listeningToProject = projectName }
		])
		def miner = new Miner(ui, stub(HistoryStorage), vcsAccess, new Measure())

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
		def vcsAccess = stub(VcsAccess, [changeEventsReaderFor: returns(stub(ChangeEventsReader))])
		def miner = new Miner(ui, stub(HistoryStorage), vcsAccess, new Measure())

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
