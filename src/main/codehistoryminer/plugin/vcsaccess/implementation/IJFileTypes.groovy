package codehistoryminer.plugin.vcsaccess.implementation

import codehistoryminer.core.vcs.reader.filetypes.FileTypes
import com.intellij.openapi.fileTypes.FileTypeManager
import vcsreader.Change

class IJFileTypes extends FileTypes {
	IJFileTypes() {
		super([])
	}

	@Override boolean isBinaryFileName(Change change) {
		def fileTypeManager = FileTypeManager.instance
		def isBinaryName = { String fileName ->
			// check for empty string because fileTypeManager considers empty file names to be binary
			!fileName.empty && fileTypeManager.getFileTypeByFileName(fileName).binary
		}
		isBinaryName(change.filePathBefore) || isBinaryName(change.filePath) || super.isBinaryFileName(change)
	}
}
