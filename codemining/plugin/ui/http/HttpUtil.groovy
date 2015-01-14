package codemining.plugin.ui.http
import com.intellij.openapi.util.io.FileUtil
import codemining.plugin.ui.UI

import static liveplugin.PluginUtil.changeGlobalVar

class HttpUtil {
	static String loadIntoHttpServer(String html, String projectName, String fileName, UI.Log log = null) {
		def tempDir = FileUtil.createTempDirectory(projectName + "_", "")
		new File("$tempDir.absolutePath/$fileName").write(html)

		log?.httpServerIsAboutToLoadHtmlFile(tempDir.absolutePath + "/" + fileName)

		def port = restartHttpServer(projectName, tempDir.absolutePath, {null}, {log?.errorOnHttpRequest(it.toString())})
		"http://localhost:${port}/${fileName}"
	}

	private static int restartHttpServer(String id, String webRootPath, Closure handler = {null}, Closure errorListener = {}) {
		changeGlobalVar(id) { previousServer ->
			if (previousServer != null) {
				previousServer.stop()
			}

			def server = new SimpleHttpServer()
			def started = false
			def serverPort = 0
			for (port in (8100..10000)) {
				try {
					server.start(port, webRootPath, handler, errorListener)
					serverPort = port
					started = true
					break
				} catch (BindException ignore) {
				}
			}
			if (!started) throw new IllegalStateException("Failed to start server '${id}'")
			serverPort
		}
	}
}

