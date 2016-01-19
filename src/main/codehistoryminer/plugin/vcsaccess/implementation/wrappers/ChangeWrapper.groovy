package codehistoryminer.plugin.vcsaccess.implementation.wrappers
import com.intellij.openapi.vcs.changes.Change as IJChange
import org.jetbrains.annotations.NotNull
import vcsreader.Change

import static codehistoryminer.core.common.langutil.Misc.withDefault

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

	@NotNull @Override Change.FileContent fileContent() {
	    if (ijChange.afterRevision == null) Change.FileContent.none
	    else new Change.FileContent(ijChange.afterRevision.content)
    }

	@NotNull @Override Change.FileContent fileContentBefore() {
	    if (ijChange.beforeRevision == null) Change.FileContent.none
	    else new Change.FileContent(ijChange.beforeRevision.content)
    }

    private static Change.Type convert(IJChange.Type changeType) {
        switch (changeType) {
            case IJChange.Type.MODIFICATION: return Change.Type.MODIFIED
            case IJChange.Type.NEW: return Change.Type.ADDED
            case IJChange.Type.DELETED: return Change.Type.DELETED
            case IJChange.Type.MOVED: return Change.Type.MOVED
            default: throw new IllegalStateException("Unknown change type: ${changeType}")
        }
    }
}

