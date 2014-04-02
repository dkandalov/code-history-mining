package analysis
import analysis.Context
import analysis.templates.AllTemplates
import analysis.templates.Template
import historystorage.EventStorage

import static analysis.Visualization.createAllVisualizations
import static analysis.templates.AllTemplates.*
import static analysis.templates.AllTemplates.template
import static java.lang.System.getenv

//generate("Code-history-mining", [
//		"full_project_name": "code-history-mining",
//		"url_to_project_page": "https://github.com/dkandalov/code-history-mining",
//		"google_drive_url": "https://drive.google.com/#folders/0B5PfR1lF8o5SUzZhbmVVS0R5WWc"
//])

//generate("JUnit", "junit", [
//		"full_project_name": "JUnit",
//		"url_to_project_page": "https://github.com/junit-team/junit",
//		"google_drive_url": "https://drive.google.com/#folders/0B5PfR1lF8o5SbUZzV1RYTC1GcDQ",
//		"code_history_dates": " from January 2001 to March 2014",
//		"committers_files_graph_comment": """
//				<br/>This particular graph is not very accurate because of different
//				VCS user names for the same person (e.g. "dsaff" and "David Saff").
//		""",
//		"wordcloud_comment": """
//		    <br/> This particular cloud might not be very representative because of commit messages
//        with meta-information (that\\'s why cloud has "threeriversinstitute" in it).
//        You can alt-click on words to exclude them.
//		"""
//], [
//		(committersChangingSameFilesGraphTemplate): { it.fill("gravity", '"Low"') }
//])

//generate("Scala", "scala", [
//		"full_project_name": "Scala programming language",
//		"url_to_project_page": "https://github.com/scala/scala‎",
//		"google_drive_url": "https://drive.google.com/#folders/0B5PfR1lF8o5SNWpwUDZJbERoMEk",
//		"code_history_dates": " from 03/01/2005 to 19/12/2013"
//], [
//		(filesInTheSameCommitGraphTemplate): { it.fill("gravity", '"High"').fill("min-link-strength", "17") },
//		(committersChangingSameFilesGraphTemplate): { it.fill("gravity", '"Low"').fill("min-cluster", "4").fill("min-link-strength", "15") },
//		(commitMessageWordCloudTemplate): { it.fill("words-to-exclude", '"si"') }
//])

//generate("Clojure", "clojure", [
//		"full_project_name": "Clojure programming language",
//		"url_to_project_page": "https://github.com/clojure/clojure",
//		"google_drive_url": "https://drive.google.com/#folders/0B5PfR1lF8o5SZ1RNUjloYldWeFE",
//		"code_history_dates": " from March 2006 to March 2013"
//], [
//		(filesInTheSameCommitGraphTemplate): { it.fill("gravity", '"Low"').fill("min-link-strength", "1") },
//		(committersChangingSameFilesGraphTemplate): { it.fill("gravity", '"Low"').fill("min-cluster", "2").fill("min-link-strength", "8") },
//		(commitMessageWordCloudTemplate): { it.fill("words-to-exclude", '"com", "stu", "signed", "stuart", "thinkrelevance", "halloway"') }
//])

//generate("GHC", "ghc", [
//		"full_project_name": "Glasgow Haskell Compiler",
//		"url_to_project_page": "https://github.com/ghc/ghc",
//		"google_drive_url": "https://drive.google.com/#folders/0B5PfR1lF8o5SaC1ncG84V29wQTQ",
//		"code_history_dates": " from January 2006 to January 2014"
//], [
//		(filesInTheSameCommitGraphTemplate): { it.fill("gravity", '"Medium"').fill("min-link-strength", "10") },
//		(committersChangingSameFilesGraphTemplate): { it.fill("gravity", '"Low"').fill("min-cluster", "2").fill("min-link-strength", "10") },
//		(timeBetweenCommitsHistogramTemplate): { it.fill("percentile", '0.6') },
//		(commitMessageWordCloudTemplate): { it.fill("words-to-exclude", '') }
//])

//generate("node.js", "node", [
//		"full_project_name": "node.js",
//		"url_to_project_page": "https://github.com/joyent/node",
//		"google_drive_url": "https://drive.google.com/#folders/0B5PfR1lF8o5SS01PdWtPUk5tQ1E",
//		"code_history_dates": " from February 2009 to January 2014"
//], [
//		(filesInTheSameCommitGraphTemplate): { it.fill("gravity", '"High"').fill("min-link-strength", "30") },
//		(committersChangingSameFilesGraphTemplate): { it.fill("gravity", '"Low"').fill("min-cluster", "2").fill("min-link-strength", "10") },
//		(timeBetweenCommitsHistogramTemplate): { it.fill("percentile", '0.6') },
//		(commitMessageWordCloudTemplate): { it.fill("words-to-exclude", '') }
//])

//generate("Ruby on Rails", "rails", [
//		"full_project_name": "Ruby on Rails",
//		"url_to_project_page": "https://github.com/rails/rails‎",
//		"google_drive_url": "https://drive.google.com/#folders/0B5PfR1lF8o5SWGRYYmlISmZtUU0",
//		"code_history_dates": " from November 2004 to January 2014"
//], [
//		(filesInTheSameCommitGraphTemplate): { it.fill("gravity", '"High"').fill("min-link-strength", "20") },
//		(committersChangingSameFilesGraphTemplate): { it.fill("gravity", '"Low"').fill("min-cluster", "4").fill("min-link-strength", "23") },
//		(timeBetweenCommitsHistogramTemplate): { it.fill("percentile", '0.6') },
//		(commitMessageWordCloudTemplate): { it.fill("words-to-exclude", '"svn", "http", "org", "commit", "rails", "trunk", "rubyonrails", "git", "ee", "ecf", "de", "fe", "id", "com"') }
//])

//generate("Ruby", "ruby-no-changelog", [
//		"full_project_name": "Ruby programming language",
//		"url_to_project_page": "https://github.com/ruby/ruby",
//		"google_drive_url": "https://drive.google.com/#folders/0B5PfR1lF8o5SYnlobWU3eVhOaDg",
//		"code_history_dates": " from January 2006 to January 2014"
//], [
//		(filesInTheSameCommitGraphTemplate): { it.fill("gravity", '"Medium"').fill("min-link-strength", "19") },
//		(committersChangingSameFilesGraphTemplate): { it.fill("gravity", '"Low"').fill("min-cluster", "2").fill("min-link-strength", "20") },
//		(timeBetweenCommitsHistogramTemplate): { it.fill("percentile", '0.6') },
//		(commitMessageWordCloudTemplate): { it.fill("words-to-exclude", '"svn", "ruby", "org", "trunk", "git", "dd", "ci", "fe", "id", "rb", "ssh", "lang"') }
//])

generate("Cucumber", "cucumber", [
		"full_project_name": "Cucumber",
		"url_to_project_page": "https://github.com/cucumber/cucumber",
		"google_drive_url": "https://drive.google.com/#folders/0B5PfR1lF8o5SaTJDYzEzUGhoUU0",
		"code_history_dates": ""
], [
		(AllTemplates.filesInTheSameCommitGraphTemplate): { it.fill("gravity", '"Medium"').fill("min-link-strength", "15") },
		(AllTemplates.committersChangingSameFilesGraphTemplate): { it.fill("gravity", '"Low"').fill("min-cluster", "2").fill("min-link-strength", "5") },
		(AllTemplates.timeBetweenCommitsHistogramTemplate): { it.fill("percentile", '0.7') },
		(AllTemplates.commitMessageWordCloudTemplate): { it.fill("words-to-exclude", '') }
])


def generate(String projectName, String fileName = projectName.toLowerCase(), Map comments, Map templateTweaks = [:]) {
	def ghPagesPath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea13/live-plugins/code-history-mining-gh-pages"
	def csvPath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea13/code-history-mining"
	def filePath = "${csvPath}/${fileName}-file-events.csv"
	def events = new EventStorage(filePath).readAllEvents({}) { line, e -> println("Failed to parse line '${line}'") }

	def template = template("all-visualizations-ghpages.html").fillProjectName(projectName)

	def visualization = createAllVisualizations(template) {
		def tweak = templateTweaks.get(it)
		tweak == null ? it : tweak(it)
	}
	def html = visualization.generate(new Context(events, projectName))
	template = withDescriptions(html)

	["code_history_dates", "committers_files_graph_comment", "wordcloud_comment"].each{
		if (!comments.containsKey(it)) comments.put(it, "")
	}
	comments.each{
		template = template.fillMustache(it.key, it.value)
	}

	new File("${ghPagesPath}/${fileName}.html").write(template.text)
}


Template withDescriptions(String html) {
	new Template(html)
			.fillMustache("overview-text", """
          This is visualization of <a href="{{url_to_project_page}}">{{full_project_name}}</a> project
          code history{{code_history_dates}}.
          It was made with <a href="https://github.com/dkandalov/code-history-mining">this IntelliJ plugin</a>.
          There is no particular method behind this so if you have an idea,
          please feel free to <a href="https://github.com/dkandalov/code-history-mining/issues/new">suggest it</a>.
          (You can find csv file with events <a href="{{google_drive_url}}">on google drive</a>.)
			""".stripMargin())
			.addBefore(span("change-size-chart"), """
					Shows the amount of changes by day/week/month. The idea is to see overall trend of the project.
          (Similar to <a href="https://help.github.com/articles/using-graphs#contributors">github contributors graph</a>
          but without interpolation.)
      """.stripMargin())
			.addBefore(span("amount-of-committers-chart"), """
					Shows how many different people committed over month/week/day.
          The idea is to see the amount of people contributing to the project.
			""".stripMargin())
			.addBefore(span("amount-of-files-in-commit-chart"), """
					Shows average amount of files in commit by day/week/month.
          Assuming that commit is a finished unit of work, the idea is to see how its size changes over time.
			""".stripMargin())
			.addBefore(span("amount-of-changing-files-chart"), """
	        Shows amount of files changed/not-changed within last week/month.
	        The idea is too see how big the change is relative to the size of codebase.
	        This can also be interpreted in the context of <a href="http://en.wikipedia.org/wiki/Open/closed_principle">open-closed principle</a>
	        as amount of "open" and "closed" classes.
			""".stripMargin())
			.addBefore(span("change-size-by-file-type-chart"), """
	        Shows amount of changes for 5 most used file types.
	        The idea is to see which languages / parts of project are used and how it evolved over time.
	        For example, how much of "java" project is really java and how much is xml/properties.
			""".stripMargin())
			.addBefore(span("files-in-the-same-commit"), """
					Shows files which were changed in the same commit several times.
          Thickness of edges is proportional to the number of commits.
	        The idea is to discover de facto dependencies between files.
	        <br/>
	        You can click on nodes to see file names in cluster.
			""".stripMargin())
			.addBefore(span("author-to-file-graph"), """
					Shows committers connections to files they have both changed within a week.
          That is if file was modified by two persons within a week, a link is added.
          The is idea is to discover how people "communicate" through changing same files.
          {{committers_files_graph_comment}}
			""".stripMargin())
			.addBefore(span("commit-treemap"), """
          Shows a break-down of commits by package/folder.
          Size of rectangles corresponds to amount of commits.
          The idea is to see which parts of the project have more attention,
          e.g. ratio between commits to production code and tests.
          (Renamed and moved package are tracked but only current package name is displayed.)
			""".stripMargin())
			.addBefore(span("commit-time-punchcard"), """
          Shows amount of commits at certain time of day (similar to
          <a href="https://help.github.com/articles/using-graphs#punchcard">github punchcard</a>).
          <br/>
          Note that you can also group commits by minute (inspired by
          <a href="http://dtrace.org/blogs/brendan/2012/03/26/subsecond-offset-heat-maps/">subsecond offset heatmaps</a>).
			""".stripMargin())
			.addBefore(span("time-between-commits-histogram"), """
          This distribution will probably be the same for any project,
          i.e. you are most likely to commit again within 5 hours or at least within a day.
          (Time is tracked separately for each committer.)
			""".stripMargin())
			.addBefore(span("commit-word-cloud"), """
          Some prepositions and articles are excluded as well as some words specific for the project but not related to code.
          The idea is to see what happened with code by observing human language.
          <br/><br/>
			""".stripMargin())
}

String span(String id) {
	"<span id=\"$id\""
}


