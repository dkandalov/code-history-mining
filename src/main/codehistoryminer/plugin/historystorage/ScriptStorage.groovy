package codehistoryminer.plugin.historystorage

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
		def wasCreated = FileUtil.createIfDoesntExist(scriptFile)
		if (!wasCreated) throw new FileNotFoundException(scriptFile.absolutePath)

		scriptFile
	}

	boolean isScriptFile(String filePath) {
		FileUtilRt.getExtension(filePath) == "groovy" &&
		FileUtil.isAncestor(new File(basePath), new File(filePath), true)
	}
}
