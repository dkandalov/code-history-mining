package codehistoryminer.plugin.historystorage

import codehistoryminer.core.lang.Misc
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessExtension
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vfs.VirtualFile
import liveplugin.implementation.Projects
import org.jetbrains.annotations.NotNull

import static liveplugin.implementation.Misc.newDisposable

class ScriptStorage {
	private final String basePath

	ScriptStorage(String basePath = null) {
		this.basePath = basePath
	}

	def init(Disposable disposable) {
		def fileWritingAccessExtension = new NonProjectFileWritingAccessExtension() {
			@Override boolean isWritable(@NotNull VirtualFile virtualFile) {
				FileUtil.isAncestor(new File(basePath), new File(virtualFile.canonicalPath), true)
			}
		}

		Projects.registerProjectListener(disposable) { Project project ->
			def area = Extensions.getArea(project)
			def extensionPoint = area.getExtensionPoint(NonProjectFileWritingAccessExtension.EP_NAME)

			extensionPoint.registerExtension(fileWritingAccessExtension)
			newDisposable([disposable, project]) {
				if (extensionPoint.hasExtension(fileWritingAccessExtension)) {
					extensionPoint.unregisterExtension(fileWritingAccessExtension)
				}
			}
		}

		this
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
