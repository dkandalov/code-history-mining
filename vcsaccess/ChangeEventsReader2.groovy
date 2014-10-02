package vcsaccess
import codemining.cli.HistoryReader
import codemining.core.common.events.FileChangeEvent
import vcsreader.Commit
import vcsreader.VcsProject

import static codemining.core.common.langutil.DateTimeUtil.dateRange
import static codemining.core.common.langutil.DateTimeUtil.floorToDay

class ChangeEventsReader2 {
	private static final Closure DEFAULT_WRAPPER = { changes, aCallback -> aCallback(changes) }

    private final VcsProject project
    private final HistoryReader historyReader
    private final def extractChangeEvents
    private final VcsAccessLog log
    private boolean lastRequestHadErrors


    ChangeEventsReader2(VcsProject project, Closure<Collection<FileChangeEvent>> extractChangeEvents = null, VcsAccessLog log = null) {
        this.project = project
        this.extractChangeEvents = extractChangeEvents
        this.log = log

        this.historyReader = new HistoryReader(new HistoryReader.Listener() {
            @Override void onFatalError(String error) {
                lastRequestHadErrors = true
            }

            @Override void onError(String error) {
                lastRequestHadErrors = true
            }
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
					consume(extractChangeEvents(commit))
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
