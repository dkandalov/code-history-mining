package vcsaccess
import codemining.core.vcs.CommitMunger
import codemining.core.vcs.CommitMungerListener
import codemining.core.vcs.FileTypes
import codemining.core.vcs.HistoryReader
import com.intellij.openapi.fileTypes.FileTypeManager
import vcsreader.Change
import vcsreader.Commit
import vcsreader.VcsProject

import static codemining.core.common.langutil.DateTimeUtil.dateRange
import static codemining.core.common.langutil.DateTimeUtil.floorToDay

class ChangeEventsReader2 {
	private static final Closure DEFAULT_WRAPPER = { changes, aCallback -> aCallback(changes) }

    private final VcsProject project
    private final HistoryReader historyReader
    private final CommitMunger commitMunger
    private final FileTypes fileTypes
    private final VcsAccessLog log
    private boolean lastRequestHadErrors


    ChangeEventsReader2(VcsProject project, VcsAccessLog log = null) {
        this.project = project
        this.log = log

        this.commitMunger = new CommitMunger(new CommitMungerListener() {
            @Override void failedToLoadContent(Change change) {
                // TODO
            }
        })

        this.historyReader = new HistoryReader(new HistoryReader.Listener() {
            @Override void onFatalError(String error) { lastRequestHadErrors = true }
            @Override void onError(String error) { lastRequestHadErrors = true }
        })

        this.fileTypes = new FileTypes(FileTypeManager.instance.registeredFileTypes.collect {
            new FileTypes.FileType(it.defaultExtension, it.binary)
        })
    }

	def readPresentToPast(Date historyStart, Date historyEnd, Closure isCancelled = null,
	                      Closure consumeWrapper = DEFAULT_WRAPPER, Closure consume) {
		request(historyStart, historyEnd, isCancelled, consumeWrapper, consume)
	}

	private request(Date historyStart, Date historyEnd, Closure isCancelled = null, Closure consumeWrapper, Closure consume) {
		def fromDate = floorToDay(historyStart)
		def toDate = floorToDay(historyEnd) + 1 // +1 because commitReader end date is exclusive

		Iterator<Commit> commits = historyReader.readHistoryFrom(project, dateRange(fromDate, toDate))

		for (commit in commits) {
			if (isCancelled?.call()) break

			consumeWrapper(commit) {
				try {
					consume(commitMunger.convertToFileChangeEvents(commit, fileTypes))
				} catch (Exception e) {
                    log?.onExtractChangeEventException(e)
                }
			}
		}
	}

	boolean getLastRequestHadErrors() {
        lastRequestHadErrors
	}
}
