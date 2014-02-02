package http
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil

import java.util.regex.Matcher

import static liveplugin.PluginUtil.changeGlobalVar

class HttpUtil {
	static SimpleHttpServer loadIntoHttpServer(String projectId, String templateFileName, String json) {
		def text = fillTemplate(readFile(templateFileName), projectId, json)

		def tempDir = FileUtil.createTempDirectory(projectId + "_", "")
		new File("$tempDir.absolutePath/$templateFileName").write(text)

		log_("Saved html file into: " + tempDir.absolutePath + "/" + templateFileName)

		restartHttpServer(projectId, tempDir.absolutePath, {null}, {log_(it.toString())})
	}

	static String fillTemplate(String templateText, String projectName, String json, Closure<String> fileReader = { readFile(it) }) {
		templateText = inlineJSLibraries(templateText, fileReader)
		templateText = fillDataPlaceholder(templateText, json)
		fillProjectNamePlaceholder(templateText, "\"$projectName\"")
	}

	private static String fillProjectNamePlaceholder(String templateText, String projectName) {
		templateText.replaceFirst(/(?s)\/\*project_name_placeholder\*\/.*\/\*project_name_placeholder\*\//, Matcher.quoteReplacement(projectName))
	}

	private static String fillDataPlaceholder(String templateText, String jsValue) {
		templateText.replaceFirst(/(?s)\/\*data_placeholder\*\/.*\/\*data_placeholder\*\//, Matcher.quoteReplacement(jsValue))
	}

	private static String inlineJSLibraries(String html, Closure<String> sourceCodeReader) {
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

	private static log_(String message) { Logger.getInstance("CodeHistoryMining").info(message) }
}

