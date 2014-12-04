package codemining.vcsaccess
import codemining.core.vcs.CommitMunger
import codemining.core.vcs.CommitReader
import codemining.core.vcs.MungedCommit
import vcsreader.Commit
import vcsreader.VcsProject

import static codemining.core.common.langutil.DateTimeUtil.dateRange
import static codemining.core.common.langutil.DateTimeUtil.floorToDay

// TODO remove
class ChangeEventsReader {
	private static final Closure DEFAULT_WRAPPER = { changes, aCallback -> aCallback(changes) }

    private final VcsProject project
    private final CommitReader commitReader
    private final CommitMunger commitMunger
    private final VcsAccessLog log
    private boolean lastRequestHadErrors


    ChangeEventsReader(VcsProject project = null, CommitMunger commitMunger = null, VcsAccessLog log = null) {
        this.project = project
        this.log = log
        this.commitMunger = commitMunger

        this.commitReader = new CommitReader(new CommitReader.Listener() {
            @Override void onFatalError(String error) {
                log.errorReadingCommits(error)
                lastRequestHadErrors = true
            }
            @Override void onError(String error) {
                log.errorReadingCommits(error)
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
		def toDate = floorToDay(historyEnd) + 1 // +1 because commitReader end date is exclusive TODO make it exclusive?

		Iterator<Commit> commits = commitReader.readFrom(project, dateRange(fromDate, toDate))

		for (commit in commits) {
			if (isCancelled?.call()) break

			consumeWrapper(commit) {
				try {
                    def mungedCommit = commitMunger.munge(new MungedCommit(commit))
                    consume(mungedCommit.fileChangeEvents)
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
