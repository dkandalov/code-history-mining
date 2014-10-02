package vcsaccess.implementation.wrappers

import vcsreader.Change
import com.intellij.openapi.vcs.changes.Change as IJChange
import static vcsreader.Change.Type

class ChangeWrapper extends Change {
    private final IJChange ijChange

    ChangeWrapper(IJChange ijChange) {
        super(
            convert(ijChange.type),
            ijChange.afterRevision.file.path,
            ijChange.beforeRevision.file.path,
            ijChange.afterRevision.revisionNumber.asString(),
            ijChange.beforeRevision.revisionNumber.asString()
        )
        this.ijChange = ijChange
    }

    @Override String content() {
        ijChange.afterRevision.content
    }

    @Override String contentBefore() {
        ijChange.beforeRevision.content
    }

    private static Type convert(IJChange.Type changeType) {
        switch (changeType) {
            case IJChange.Type.MODIFICATION: return Type.MODIFICATION
            case IJChange.Type.NEW: return Type.NEW
            case IJChange.Type.DELETED: return Type.DELETED
            case IJChange.Type.MOVED: return Type.MOVED
            default: throw new IllegalStateException("Unknown change type: ${changeType}")
        }
    }
}

