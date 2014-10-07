package vcsaccess.implementation.wrappers
import com.intellij.openapi.vcs.changes.Change as IJChange
import vcsreader.Change

@SuppressWarnings("UnnecessaryQualifiedReference") // because IntelliJ doesn't understand that import is required by groovy
class ChangeWrapper extends Change {
    static ChangeWrapper none = null
    private final IJChange ijChange

    static ChangeWrapper create(IJChange ijChange, String commonVcsRoot) {
        def notUnderCommonRoot = { !it?.startsWith(commonVcsRoot) }
        def trimCommonRoot = { String path ->
            path?.startsWith(commonVcsRoot) ? path.replace(commonVcsRoot, "") : path
        }

        def filePath = ijChange.afterRevision?.file?.path
        def filePathBefore = ijChange.beforeRevision?.file?.path
        if (notUnderCommonRoot(filePath) && notUnderCommonRoot(filePathBefore)) return none

        new ChangeWrapper(
                ijChange,
                convert(ijChange.type),
                withDefault(noFilePath, trimCommonRoot(filePath)),
                withDefault(noFilePath, trimCommonRoot(filePathBefore)),
                withDefault(noRevision, ijChange.afterRevision?.revisionNumber?.asString()),
                withDefault(noRevision, ijChange.beforeRevision?.revisionNumber?.asString())
        )
    }

    private ChangeWrapper(IJChange ijChange, Change.Type type, String filePath, String filePathBefore, String revision, String revisionBefore) {
        super(type, filePath, filePathBefore, revision, revisionBefore)
        this.ijChange = ijChange
    }

    @Override String content() {
        ijChange.afterRevision?.content
    }

    @Override String contentBefore() {
        ijChange.beforeRevision?.content
    }

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

