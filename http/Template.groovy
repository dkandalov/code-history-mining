package http

import java.util.regex.Matcher

class Template {
	final String text

	Template(String text) {
		this.text = text
	}

	Template fillProjectName(String projectName) {
		new Template(fillProjectNamePlaceholder(text, "\"$projectName\""))
	}

	Template fillData(String jsValue) {
		new Template(fillDataPlaceholder(text, jsValue))
	}

	Template inlineImports(Closure<String> readFile) {
		new Template(inlineJSLibraries(inlineStylesheets(text, readFile), readFile))
	}

	Template appendAt(String insertPoint, String s) {
		new Template(text.replace(insertPoint, s + insertPoint))
	}

	String contentOfLastTag(String tagName) {
		def openTag = "<$tagName>"
		def closeTag = "</$tagName>"

		def from = text.lastIndexOf(openTag)
		if (from == -1) return ""

		def to = text.indexOf(closeTag, from)
		if (to == -1) return ""

		text.substring(from, to + closeTag.length())
	}


	private static String fillProjectNamePlaceholder(String templateText, String projectName) {
		templateText.replaceFirst(/(?s)\/\*project_name_placeholder\*\/.*\/\*project_name_placeholder\*\//, Matcher.quoteReplacement(projectName))
	}

	private static String fillDataPlaceholder(String templateText, String jsValue) {
		templateText.replaceFirst(/(?s)\/\*data_placeholder\*\/.*\/\*data_placeholder\*\//, Matcher.quoteReplacement(jsValue))
	}

	private static String inlineStylesheets(String html, Closure<String> fileReader) {
		(html =~ /(?sm).*?<link.*? rel="stylesheet".*? href="(.*?)".*/).with{
			if (!matches()) html
			else inlineStylesheets(
					html.replaceFirst(/(?sm)\n*\s*?<link.* rel="stylesheet".* href="(${group(1)})".*?>/, "")
							.replaceFirst(/\s*?<\/head>/, "<style>${fileReader(group(1))}</style></head>"),
					fileReader
			)
		}
	}

	private static String inlineJSLibraries(String html, Closure<String> fileReader) {
		(html =~ /(?sm).*?<script src="(.*?)"><\/script>.*/).with{
			if (!matches()) html
			else inlineJSLibraries(
					html.replace("<script src=\"${group(1)}\"></script>", "<script>${fileReader(group(1))}</script>"),
					fileReader
			)
		}
	}
}
