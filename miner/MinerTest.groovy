package miner
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import historystorage.EventStorage
import historystorage.HistoryStorage
import org.junit.Test
import util.Measure
import vcsaccess.ChangeEventsReader
import vcsaccess.HistoryGrabberConfig
import vcsaccess.VcsAccess

import static util.DateTimeUtil.date
import static util.DateTimeUtil.exactDateTime
import static util.GroovyStubber.*

class MinerTest {
//	@Test def "on VCS update does nothing if already grabbed on this date"() {
//		def aProject = stubProject([getName: returns("aProject")])
//		def storage = stubHistoryStorage([
//				lastGrabTime: returns(dateTime("09:00 23/11/2012"))
//		])
//		def vcsAccess = stubVcsAccess([
//				changeEventsReaderFor: returns(stubChangeEventsReader()),
//		])
//		def miner = new Miner(stubUI(), storage, vcsAccess, new Measure())
//
//		miner.grabHistoryOnVcsUpdate(aProject.name, dateTime("13:00 23/11/2012"))
//	}

	@Test def "on VCS update grabs history from latest event in file history util today"() {
		// given
		Date from = null
		Date to = null
		def historyStorage = stub(HistoryStorage, [
				eventStorageFor: returns(stub(EventStorage, [
						getMostRecentEventTime: returns(date("20/11/2012"))
				])),
				loadGrabberConfigFor: returns(someConfig)
		])
		def changeEventReader = stub(ChangeEventsReader, [
				readPastToPresent: { Date historyStart, Date historyEnd, isCancelled, consumeWrapper, consume ->
					from = historyStart
					to = historyEnd
				}
		])
		def vcsAccess = stub(VcsAccess, [changeEventsReaderFor: returns(changeEventReader)])
		def ui = stub(UI, [runInBackground: {taskDescription, closure -> closure([:] as ProgressIndicator)}])
		def miner = new Miner(ui, historyStorage, vcsAccess, new Measure())

		// when / then
		miner.grabHistoryOnVcsUpdate(someProject, date("23/11/2012"))
		assert from == exactDateTime("00:00:01 20/11/2012") // TODO test this in integration tests
		assert to == date("23/11/2012")
	}

	@Test def "on grab history should register VCS update listener"() {
		// given
		def aProject = stub(Project, [getName: returns("aProject")])
		def listeningToProject = ""
		def ui = stub(UI, [
				showGrabbingDialog: { config, project, Closure onOkCallback ->
					def grabOnVcsUpdate = true
					onOkCallback(new HistoryGrabberConfig(new Date() - 300, new Date(), "some.csv", false, grabOnVcsUpdate))
				}
		])
		def vcsAccess = stub(VcsAccess, [
				changeEventsReaderFor: returns(stub(ChangeEventsReader)),
				addVcsUpdateListenerFor: { String projectName, listener -> listeningToProject = projectName }
		])
		def miner = new Miner(ui, stub(HistoryStorage), vcsAccess, new Measure())

		// when / then
		miner.grabHistoryOf(aProject)
		assert listeningToProject == aProject.name
	}

	@Test void "should only grab history of one project at a time"() {
		// given
		def showedGrabberDialog = 0
		def showedGrabbingInProgress = 0
		def ui = stub(UI, [
				runInBackground: doesNothing,
				showGrabbingDialog: { config, project, Closure onOkCallback ->
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


	private static final Project someProject = stub(Project)
	private static final someConfig = new HistoryGrabberConfig(new Date() - 300, new Date(), "some.csv", false, false)
}
