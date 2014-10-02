package vcsaccess.implementation.wrappers
import com.intellij.openapi.vcs.changes.Change as IJChange
import vcsreader.Change

class ChangeWrapper extends Change {
    private final IJChange ijChange

    ChangeWrapper(IJChange ijChange) {
        super(
            convert(ijChange.type),
            withDefault(noFilePath, ijChange.afterRevision?.file?.path),
            withDefault(noFilePath, ijChange.beforeRevision?.file?.path),
            withDefault(noRevision, ijChange.afterRevision?.revisionNumber?.asString()),
            withDefault(noRevision, ijChange.beforeRevision?.revisionNumber?.asString())
        )
        this.ijChange = ijChange
    }

    @Override String content() {
        ijChange.afterRevision.content
    }

    @Override String contentBefore() {
        ijChange.beforeRevision.content
    }

    @SuppressWarnings("UnnecessaryQualifiedReference") // because IntelliJ doesn't understand that import is required by groovy
    private static Change.Type convert(IJChange.Type changeType) {
        switch (changeType) {
            case IJChange.Type.MODIFICATION: return Change.Type.MODIFICATION
            case IJChange.Type.NEW: return Change.Type.NEW
            case IJChange.Type.DELETED: return Change.Type.DELETED
            case IJChange.Type.MOVED: return Change.Type.MOVED
            default: throw new IllegalStateException("Unknown change type: ${changeType}")
        }
    }

    // TODO move to langutil
    static <T> T withDefault(T defaultValue, T value) { value == null ? defaultValue : value }
}

