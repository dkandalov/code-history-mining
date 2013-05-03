package http
import com.intellij.openapi.util.io.FileUtil

import java.util.regex.Matcher

import static intellijeval.PluginUtil.changeGlobalVar
import static intellijeval.PluginUtil.log

class HttpUtil {
	static SimpleHttpServer loadIntoHttpServer(String projectId, String pathToTemplates, String templateFileName, String json) {
		def tempDir = FileUtil.createTempDirectory(projectId + "_", "")

		def text = readFile("$pathToTemplates/$templateFileName")
		text = inlineD3js(text, readFile(pathToTemplates + "/d3.v3.min.js"))
		text = fillDataPlaceholder(text, json)
		new File("$tempDir.absolutePath/$templateFileName").write(text)

		log("Saved tree map into: " + tempDir.absolutePath + "/" + templateFileName)

		restartHttpServer(projectId, tempDir.absolutePath, {null}, {log(it)})
	}

	private static String fillDataPlaceholder(String templateText, String jsValue) {
		templateText.replaceFirst(/(?s)\/\*data_placeholder\*\/.*\/\*data_placeholder\*\//, Matcher.quoteReplacement(jsValue))
	}

	private static String inlineD3js(String templateText, String d3js) {
		templateText.replace("<script src=\"d3.v3.min.js\"></script>", "<script>$d3js</script>")
	}

	private static String readFile(String template) {
		new File(template).readLines().join("\n")
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

