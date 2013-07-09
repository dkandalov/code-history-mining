package http
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil

import java.util.regex.Matcher

import static liveplugin.PluginUtil.changeGlobalVar

class HttpUtil {
	static SimpleHttpServer loadIntoHttpServer(String projectId, String templateFileName, String json) {
		def tempDir = FileUtil.createTempDirectory(projectId + "_", "")

		def text = readFile(templateFileName)
		text = inlineJSLibraries(text) { jsLibFileName -> readFile(jsLibFileName) }
		text = fillDataPlaceholder(text, json)
		text = fillProjectNamePlaceholder(text, "\"$projectId\"")
		new File("$tempDir.absolutePath/$templateFileName").write(text)

		log_("Saved html file into: " + tempDir.absolutePath + "/" + templateFileName)

		restartHttpServer(projectId, tempDir.absolutePath, {null}, {log_(it.toString())})
	}

	static String fillProjectNamePlaceholder(String templateText, String projectName) {
		templateText.replaceFirst(/(?s)\/\*project_name_placeholder\*\/.*\/\*project_name_placeholder\*\//, Matcher.quoteReplacement(projectName))
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

	private static String readFile(String fileName) {
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

	static log_(String message) { Logger.getInstance("CodeHistoryMining").info(message) }
}

