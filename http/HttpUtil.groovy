package http
import com.intellij.openapi.util.io.FileUtil

import java.util.regex.Matcher

import static intellijeval.PluginUtil.changeGlobalVar
import static intellijeval.PluginUtil.log

class HttpUtil {
	static SimpleHttpServer loadIntoHttpServer(String projectId, String pathToTemplates, String templateFileName, String json) {
		def tempDir = FileUtil.createTempDirectory(projectId + "_", "")

		def text = readFile("$pathToTemplates/$templateFileName")
		text = inlineJSLibraries(text) { jsLibFileName -> readFile(pathToTemplates + "/" + jsLibFileName) }
		text = fillDataPlaceholder(text, json)
		new File("$tempDir.absolutePath/$templateFileName").write(text)

		log("Saved tree map into: " + tempDir.absolutePath + "/" + templateFileName)

		restartHttpServer(projectId, tempDir.absolutePath, {null}, {log(it)})
	}

	static String fillDataPlaceholder(String templateText, String jsValue) {
		templateText.replaceFirst(/(?s)\/\*data_placeholder\*\/.*\/\*data_placeholder\*\//, Matcher.quoteReplacement(jsValue))
	}

	static String inlineJSLibraries(String html, Closure<String> sourceCodeReader) {
		(html =~ /(?sm).*?<script src="(.*?)"><\/script>.*/).with{
			if (!matches()) html
			else inlineJSLibraries(
					html.replace("<script src=\"${group(1)}\"></script>", "<script>${sourceCodeReader(group(1))}</script>"),
					sourceCodeReader
			)
		}
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

