package codehistoryminer.historystorage

import com.intellij.openapi.util.io.FileUtil

class QueryScriptsStorage {
	private final String basePath

	QueryScriptsStorage(String basePath = null) {
		this.basePath = basePath
	}

	File findOrCreateScriptFile(String id) {
		def scriptsFolder = new File(basePath)
		FileUtil.createDirectory(scriptsFolder)

		def scriptFile = new File(scriptsFolder.absolutePath + File.separator + id + ".groovy")
		def wasCreated = FileUtil.createIfDoesntExist(scriptFile)
		if (!wasCreated) throw new FileNotFoundException(scriptFile.absolutePath)

		scriptFile
	}
}
