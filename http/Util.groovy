package http
import com.intellij.openapi.util.io.FileUtil

import java.util.regex.Matcher

import static intellijeval.PluginUtil.changeGlobalVar
import static intellijeval.PluginUtil.log

class Util {
	static SimpleHttpServer loadIntoHttpServer(String projectId, String pathToHttpFiles, String templateName, String json) {
		def tempDir = FileUtil.createTempDirectory(projectId + "_", "_treemap")
		FileUtil.copyDirContent(new File(pathToHttpFiles), tempDir) // TODO make templates self-contained
		fillTemplate("$pathToHttpFiles/$templateName", json, tempDir.absolutePath + "/treemap.html")

		log("Saved tree map into: " + tempDir.absolutePath)

		restartHttpServer(projectId, tempDir.absolutePath, {null}, {log(it)})
	}

	private static void fillTemplate(String template, String jsValue, String pathToNewFile) {
		def templateText = new File(template).readLines().join("\n")
		def text = templateText.replaceFirst(/(?s)\/\*data_placeholder\*\/.*\/\*data_placeholder\*\//, Matcher.quoteReplacement(jsValue))
		new File(pathToNewFile).write(text)
	}

	private static SimpleHttpServer restartHttpServer(String id, String webRootPath, Closure handler = {null}, Closure errorListener = {}) {
		changeGlobalVar(id) { previousServer ->
			if (previousServer != null) {
				previousServer.stop()
			}

			def server = new SimpleHttpServer()
			def started = false
			for (port in (8100..10000)) {
				try {
					server.start(port, webRootPath, handler, errorListener)
					started = true
					break
				} catch (BindException ignore) {
				}
			}
			if (!started) throw new IllegalStateException("Failed to start server '${id}'")
			server
		}
	}
}

