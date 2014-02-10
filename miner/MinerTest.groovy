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

	@Test def "on VCS update grabs history from now to latest event in file history"() {
		// given
		Date from = null
		Date to = null
		def eventStorage = stubEventStorage([
				getMostRecentEventTime: returns(date("20/11/2012"))
		])
		def historyStorage = stubHistoryStorage([
				eventStorageFor: returns(eventStorage),
				loadGrabberConfigFor: returns(someConfig)
		])
		def changeEventReader = stubChangeEventsReader([
				readPastToPresent: { Date historyStart, Date historyEnd, isCancelled, consumeWrapper, consume ->
					from = historyStart
					to = historyEnd
				}
		])
		def vcsAccess = stubVcsAccess([changeEventsReaderFor: returns(changeEventReader)])
		def ui = stubUI([runInBackground: {taskDescription, closure -> closure([:] as ProgressIndicator)}])
		def miner = new Miner(ui, historyStorage, vcsAccess, new Measure())

		// when / then
		miner.grabHistoryOnVcsUpdate(someProject, date("23/11/2012"))
		assert from == exactDateTime("00:00:01 20/11/2012") // TODO test this in integration tests
		assert to == date("24/11/2012")
	}

	@Test def "on grab history should register VCS update listener"() {
		// given
		def aProject = stubProject([getName: returns("aProject")])
		def listeningToProject = ""
		def ui = stubUI([
				showGrabbingDialog: { config, project, Closure onOkCallback ->
					def grabOnVcsUpdate = true
					onOkCallback(new HistoryGrabberConfig(new Date() - 300, new Date(), "some.csv", false, grabOnVcsUpdate))
				}
		])
		def vcsAccess = stubVcsAccess([
				changeEventsReaderFor: returns(stubChangeEventsReader()),
				addVcsUpdateListenerFor: { String projectName, listener -> listeningToProject = projectName }
		])
		def miner = new Miner(ui, stubHistoryStorage(), vcsAccess, new Measure())

		// when / then
		miner.grabHistoryOf(aProject)
		assert listeningToProject == aProject.name
	}

	@Test void "should only grab history of one project at a time"() {
		// given
		def showedGrabberDialog = 0
		def showedGrabbingInProgress = 0
		def ui = stubUI([
				runInBackground: doesNothing,
				showGrabbingDialog: { config, project, Closure onOkCallback ->
					showedGrabberDialog++
					onOkCallback(someConfig)
				},
				showGrabbingInProgressMessage: does{ showedGrabbingInProgress++ },
		])
		def vcsAccess = stubVcsAccess([changeEventsReaderFor: returns(stubChangeEventsReader())])
		def miner = new Miner(ui, stubHistoryStorage(), vcsAccess, new Measure())

		// when / then
		miner.grabHistoryOf(someProject)
		assert showedGrabberDialog == 1
		assert showedGrabbingInProgress == 0

		miner.grabHistoryOf(someProject)
		assert showedGrabberDialog == 1
		assert showedGrabbingInProgress == 1
	}

	private static UI stubUI(Map map = [:]) {
		def defaultMap = [runInBackground: doesNothing, showGrabbingDialog: doesNothing,
				showGrabbingInProgressMessage: doesNothing, showGrabbingFinishedMessage: doesNothing]
		defaultMap.putAll(map)
		defaultMap as UI
	}

	private static VcsAccess stubVcsAccess(Map map = [:]) {
		def defaultMap = [noVCSRootsIn: returns(false), changeEventsReaderFor: returns(null), addVcsUpdateListenerFor: doesNothing]
		defaultMap.putAll(map)
		defaultMap as VcsAccess
	}

	private static HistoryStorage stubHistoryStorage(Map map = [:]) {
		def defaultMap = [loadGrabberConfigFor: returns(null), saveGrabberConfigFor: doesNothing]
		defaultMap.putAll(map)
		defaultMap as HistoryStorage
	}

	private static EventStorage stubEventStorage(Map map = [:]) {
		def defaultMap = [getOldestEventTime: returns(null), getMostRecentEventTime: returns(null), hasNoEvents: returns(false)]
		defaultMap.putAll(map)
		defaultMap as EventStorage
	}

	private static ChangeEventsReader stubChangeEventsReader(Map map = [:]) {
		def defaultMap = [readPresentToPast: doesNothing, readPastToPresent: doesNothing, getLastRequestHadErrors: returns(false)]
		defaultMap.putAll(map)
		defaultMap as ChangeEventsReader
	}

	private static Project stubProject(Map map = [:]) {
		def defaultMap = [getName: returns(null)]
		defaultMap.putAll(map)
		defaultMap as Project
	}

	private static <T> Closure<T> returns(T value) {
		{ Object... args -> value }
	}

	private static <T> Closure<T> does(Closure closure) {
		{ Object... args -> closure() }
	}

	private static final Project someProject = stubProject()
	private static final someConfig = new HistoryGrabberConfig(new Date() - 300, new Date(), "some.csv", false, false)
	private static final Closure doesNothing = { Object... args -> }
}
