package miner

import com.intellij.openapi.project.Project
import historystorage.HistoryStorage
import org.junit.Test
import util.Measure
import vcsaccess.ChangeEventsReader
import vcsaccess.HistoryGrabberConfig
import vcsaccess.VcsAccess

class MinerTest {

	@Test void "should only grab history of one project at a time"() {
		// given
		def showedGrabberDialog = 0
		def showedGrabbingInProgress = 0
		def ui = [
				runInBackground: doesNothing,
				showGrabbingDialog: { config, project, Closure onOkCallback ->
					showedGrabberDialog++
					onOkCallback(someConfig)
				},
				showGrabbingInProgressMessage: does{ showedGrabbingInProgress++ },
				showGrabbingFinishedMessage: doesNothing,
		] as UI
		def miner = new Miner(ui, dummyStorage(), vcsAccessWith(dummyChangeEventsReader()), new Measure())

		// when / then
		miner.grabHistoryOf(someProject)
		assert showedGrabberDialog == 1
		assert showedGrabbingInProgress == 0

		miner.grabHistoryOf(someProject)
		assert showedGrabberDialog == 1
		assert showedGrabbingInProgress == 1
	}

	private static VcsAccess vcsAccessWith(ChangeEventsReader changeEventsReader) {
		[noVCSRootsIn: returns(false), changeEventsReaderFor: returns(changeEventsReader)] as VcsAccess
	}

	private static HistoryStorage dummyStorage() {
		[loadGrabberConfigFor: returns(someConfig), saveGrabberConfigFor: doesNothing] as HistoryStorage
	}

	private static ChangeEventsReader dummyChangeEventsReader() {
		[readPresentToPast: doesNothing, readPastToPresent: doesNothing, getLastRequestHadErrors: returns(false)] as ChangeEventsReader
	}

	private static <T> Closure<T> returns(T value) {
		{ Object... args -> value }
	}

	private static <T> Closure<T> does(Closure closure) {
		{ Object... args -> closure() }
	}

	private static final Project someProject = [:] as Project
	private static final someConfig = new HistoryGrabberConfig(new Date() - 300, new Date(), "some.csv", false, false)
	private static final Closure doesNothing = { Object... args -> }
}
