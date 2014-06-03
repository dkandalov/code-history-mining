package vcsaccess
import com.intellij.openapi.vcs.VcsRoot
import com.intellij.openapi.vcs.versionBrowser.CommittedChangeList as Commit
import common.events.FileChangeEvent
import vcsaccess.implementation.CommitReader

import static common.langutil.DateTimeUtil.floorToDay

class ChangeEventsReader {
	private static final Closure DEFAULT_WRAPPER = { changes, aCallback -> aCallback(changes) }

	private final List<VcsRoot> vcsRoots
	private final CommitReader commitReader
	private final def extractChangeEvents
    private final VcsAccessLog log

    ChangeEventsReader(List<VcsRoot> vcsRoots = [], CommitReader commitReader = null,
                       Closure<Collection<FileChangeEvent>> extractChangeEvents = null, VcsAccessLog log = null) {
        this.log = log
        this.vcsRoots = vcsRoots
		this.commitReader = commitReader
		this.extractChangeEvents = extractChangeEvents
	}

	def readPresentToPast(Date historyStart, Date historyEnd, Closure isCancelled = null,
	                      Closure consumeWrapper = DEFAULT_WRAPPER, Closure consume) {
		request(historyStart, historyEnd, isCancelled, true, consumeWrapper, consume)
	}

	def readPastToPresent(Date historyStart, Date historyEnd, Closure isCancelled = null,
	                      Closure consumeWrapper = DEFAULT_WRAPPER, Closure consume) {
		request(historyStart, historyEnd, isCancelled, false, consumeWrapper, consume)
	}

	private request(Date historyStart, Date historyEnd, Closure isCancelled = null, boolean readPresentToPast,
	            Closure consumeWrapper, Closure consume) {
		def fromDate = floorToDay(historyStart)
		def toDate = floorToDay(historyEnd) + 1 // +1 because commitReader end date is exclusive

		Iterator<Commit> commits = commitReader.readCommits(fromDate, toDate, readPresentToPast, vcsRoots)

		for (commit in commits) {
			if (commit == CommitReader.NO_MORE_COMMITS) break
			if (isCancelled?.call()) break
			if (commit.commitDate < historyStart || commit.commitDate > historyEnd) continue
			if (readPresentToPast && commit.commitDate == historyEnd) continue
			if (!readPresentToPast && commit.commitDate == historyStart) continue

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
		commitReader.lastRequestHadErrors
	}
}
