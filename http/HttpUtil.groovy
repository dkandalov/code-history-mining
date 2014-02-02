package http
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil

import static liveplugin.PluginUtil.changeGlobalVar

class HttpUtil {
	static SimpleHttpServer loadIntoHttpServer(String html, String projectName, String templateFileName) {
		def tempDir = FileUtil.createTempDirectory(projectName + "_", "") // TODO use randomly named temp file
		new File("$tempDir.absolutePath/$templateFileName").write(html)

		log_("Saved html file into: " + tempDir.absolutePath + "/" + templateFileName)

		restartHttpServer(projectName, tempDir.absolutePath, {null}, {log_(it.toString())})
	}

	static String readFile(String fileName) {
		FileUtil.loadTextAndClose(HttpUtil.class.getResourceAsStream("/templates/$fileName"))
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

	private static log_(String message) { Logger.getInstance("CodeHistoryMining").info(message) }
}

