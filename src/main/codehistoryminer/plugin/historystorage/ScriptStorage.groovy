package codehistoryminer.plugin.historystorage

import codehistoryminer.core.lang.Misc
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt

class ScriptStorage {
	private final String basePath

	ScriptStorage(String basePath = null) {
		this.basePath = basePath
	}

	File findOrCreateScriptFile(String fileName) {
		def scriptsFolder = new File(basePath)
		FileUtil.createDirectory(scriptsFolder)

		def scriptFile = new File(scriptsFolder.absolutePath + File.separator + fileName)
		def isNewFile = !scriptFile.exists()
		if (isNewFile) {
			def wasCreated = FileUtil.createIfDoesntExist(scriptFile)
			if (!wasCreated) throw new FileNotFoundException(scriptFile.absolutePath)
			scriptFile.write(newScriptContent(), Misc.UTF8.name())
		}
		scriptFile
	}

	boolean isScriptFile(String filePath) {
		FileUtilRt.getExtension(filePath) == "groovy" &&
		FileUtil.isAncestor(new File(basePath), new File(filePath), true)
	}

	private static String newScriptContent() {
		"""
// To run the script use alt+shift+E (or "Run Code History Script" in editor context menu).
// For more details about scripts and examples see GitHub wiki
// https://github.com/dkandalov/code-history-mining/wiki/Code-History-Script-API.

data.size()
"""
	}
}
