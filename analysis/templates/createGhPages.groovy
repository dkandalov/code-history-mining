package analysis.templates

import analysis.Context
import historystorage.EventStorage

import static analysis.Visualization.createAllVisualizations
import static analysis.templates.AllTemplates.template
import static java.lang.System.getenv

generate("Code-history-mining", [
		"full_project_name": "code-history-mining",
		"url_to_project_page": "https://github.com/dkandalov/code-history-mining",
		"google_drive_url": "https://drive.google.com/#folders/0B5PfR1lF8o5SUzZhbmVVS0R5WWc",
		"code_history_dates": ""
])


def generate(String projectName, Map comments) {
	def ghPagesPath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea13/live-plugins/code-history-mining-gh-pages"
	def csvPath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea13/code-history-mining"
	def filePath = "${csvPath}/${projectName}-file-events.csv"
	def events = new EventStorage(filePath).readAllEvents({}) { line, e -> println("Failed to parse line '${line}'") }

	def template = template("all-visualizations-ghpages.html").fillProjectName(projectName)

	def visualization = createAllVisualizations(template)
	def html = visualization.generate(new Context(events, projectName))
	template = new Template(html)
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

	comments.each{
		template = template.fillMustache(it.key, it.value)
	}

	new File("${ghPagesPath}/${projectName}.html").write(template.text)
}

String span(String id) {
	"<span id=\"$id\">"
}


