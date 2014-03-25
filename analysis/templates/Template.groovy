package analysis.templates

import groovy.transform.Immutable

import static java.util.regex.Matcher.quoteReplacement

@Immutable
class Template {
	final String text

	Template fillProjectName(String projectName) {
		new Template(fillProjectNamePlaceholder(text, projectName))
	}

	Template fillData(String jsValue) {
		fill("data_placeholder", jsValue)
	}

	Template fill(String placeholder, String value) {
		new Template(fillJsPlaceholder(placeholder, value, text))
	}

	Template inlineImports(Closure<String> readFile) {
		new Template(inlineJSLibraries(inlineStylesheets(text, readFile), readFile))
	}

	Template addBefore(String marker, String textToAdd) {
		new Template(text.replace(marker, textToAdd + marker))
	}

	Template removeJsAddedHeader() {
		new Template(text
				.replaceAll(/(?m)var headerSpan.*?;/, "")
				.replaceAll(/(?m)headerSpan\..*?;/, "")
				.replaceAll(/(?m)newHeader\..*?;/, "")
		)
	}

	Template width(int value) {
		new Template(text
				.replaceAll(/width =.*?,/, "width = ${value},")
				.replaceAll(/width:.*?margin.*?,/, "width: ${value},")
		)
	}

	Template fillMustache(String name, String withText) {
		new Template(fillMustachePlaceholder(name, withText, text))
	}

	List<String> allTags(String tagName) {
		def openTag = "<$tagName>"
		def closeTag = "</$tagName>"
		def from = -1
		def result = []

		while (true) {
			from = text.indexOf(openTag, from + 1)
			if (from == -1) break
			def to = text.indexOf(closeTag, from)
			if (to == -1) break
			result << text.substring(from, to + closeTag.length())
		}
		result
	}

	String lastTag(String tagName) {
		def openTag = "<$tagName>"
		def closeTag = "</$tagName>"

		def from = text.lastIndexOf(openTag)
		if (from == -1) return ""

		def to = text.indexOf(closeTag, from)
		if (to == -1) return ""

		text.substring(from, to + closeTag.length())
	}

	String contentOfTag(String tagName) {
		def tag = lastTag(tagName)
		if (tag.empty) ""
		else tag.replace("<$tagName>", "").replace("</$tagName>", "")
	}

	String getMainTagId() {
		(text =~ /(?s).*<span id="(.*?)"><\/span>.*/).with {
			matches() ? group(1) : ""
		}
	}


	private static String fillProjectNamePlaceholder(String inText, String withProjectName) {
		def text = fillJsPlaceholder("project_name_placeholder", "\"$withProjectName\"", inText)
		fillMustachePlaceholder("project-name", withProjectName, text)
	}


	private static String fillJsPlaceholder(String name, String withJsValue, String inText) {
		inText.replaceAll(/(?s)\/\*${name}\*\/.*\/\*${name}\*\//, quoteReplacement(withJsValue))
	}

	private static String fillMustachePlaceholder(String name, String withText, String inText) {
		inText.replace("{{${name}}}", withText)
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
