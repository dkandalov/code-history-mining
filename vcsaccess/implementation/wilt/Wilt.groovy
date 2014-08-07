package vcsaccess.implementation.wilt

/**
 * Calculates Whitespace Integrated over Lines of Text (WILT).
 * (Idea by Robert Smallshire, as described in these slides http://goo.gl/hF1DeF
 * and this talk https://vimeo.com/user22258446/review/79099671/d12d153d71)
 */
class Wilt {
	static complexityBefore = { context ->
		String textBeforeChange = context.change.beforeRevision?.content
		textBeforeChange == null ? 0 : complexityOf(textBeforeChange)
	}
	static complexityAfter = { context ->
		String textAfterChange = context.change.afterRevision?.content
		textAfterChange == null ? 0 : complexityOf(textAfterChange)
	}

	static int complexityOf(String someCode, int spacesInIndent = 4) {
		complexityByLineOf(someCode, spacesInIndent).sum(0){ it[0] } as int
	}

	static List complexityByLineOf(String someCode, int spacesInIndent) {
		someCode
			.split("\n").findAll{ !it.trim().empty }
			.collect{ line ->
				[indentationOf(line, spacesInIndent), line]
			}
	}

	private static int indentationOf(String line, int spacesInIndent) {
		line = line.replaceAll("\t", " " * spacesInIndent)
		int i = 0
		while (i < line.length() && line[i] == ' ') i++
		i / spacesInIndent
	}
}
