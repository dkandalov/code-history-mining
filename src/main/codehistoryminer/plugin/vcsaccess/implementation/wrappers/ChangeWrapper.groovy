package codehistoryminer.plugin.vcsaccess.implementation.wrappers
import com.intellij.openapi.vcs.changes.Change as IJChange
import org.jetbrains.annotations.NotNull
import vcsreader.Change
import vcsreader.VcsChange

import static codehistoryminer.core.lang.Misc.withDefault

@SuppressWarnings("UnnecessaryQualifiedReference") // because IntelliJ doesn't understand that import is required by groovy
class ChangeWrapper implements VcsChange {
    static ChangeWrapper none = null
    private final IJChange ijChange
	private Change change

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

    private ChangeWrapper(IJChange ijChange, VcsChange.Type type, String filePath, String filePathBefore, String revision, String revisionBefore) {
	    this.change = new Change(type, filePath, filePathBefore, revision, revisionBefore)
	    this.ijChange = ijChange
    }

	@NotNull @Override VcsChange.Type getType() {
		change.type
	}

	@NotNull @Override String getFilePath() {
		change.filePath
	}

	@NotNull @Override String getFilePathBefore() {
		change.filePathBefore
	}

	@Override String getRevision() {
		change.revision
	}

	@Override String getRevisionBefore() {
		change.revisionBefore
	}

	@NotNull @Override VcsChange.FileContent fileContent() {
	    if (ijChange.afterRevision == null) VcsChange.FileContent.none
	    else new VcsChange.FileContent(ijChange.afterRevision.content)
    }

	@NotNull @Override VcsChange.FileContent fileContentBefore() {
	    if (ijChange.beforeRevision == null) VcsChange.FileContent.none
	    else new VcsChange.FileContent(ijChange.beforeRevision.content)
    }

    private static VcsChange.Type convert(IJChange.Type changeType) {
        switch (changeType) {
            case IJChange.Type.MODIFICATION: return VcsChange.Type.MODIFIED
            case IJChange.Type.NEW: return VcsChange.Type.ADDED
            case IJChange.Type.DELETED: return VcsChange.Type.DELETED
            case IJChange.Type.MOVED: return VcsChange.Type.MOVED
            default: throw new IllegalStateException("Unknown change type: ${changeType}")
        }
    }
}

