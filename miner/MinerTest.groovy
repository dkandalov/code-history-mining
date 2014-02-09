package miner

import com.intellij.openapi.project.Project
import historystorage.HistoryStorage
import org.junit.Test
import util.Measure
import vcsaccess.ChangeEventsReader
import vcsaccess.HistoryGrabberConfig
import vcsaccess.VcsAccess

class MinerTest {
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
		def miner = new Miner(ui, stubStorage(), vcsAccess, new Measure())

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
		def miner = new Miner(ui, stubStorage(), vcsAccess, new Measure())

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
		def defaultMap = [noVCSRootsIn: returns(false), changeEventsReaderFor: returns(null)]
		defaultMap.putAll(map)
		defaultMap as VcsAccess
	}

	private static HistoryStorage stubStorage() {
		[loadGrabberConfigFor: returns(null), saveGrabberConfigFor: doesNothing] as HistoryStorage
	}

	private static ChangeEventsReader stubChangeEventsReader() {
		[readPresentToPast: doesNothing, readPastToPresent: doesNothing, getLastRequestHadErrors: returns(false)] as ChangeEventsReader
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
