package analysis.templates
import analysis.Context
import historystorage.EventStorage

import static analysis.Visualization.createAllVisualizations
import static analysis.templates.AllTemplates.template
import static java.lang.System.getenv

def projectName = "code-history-mining"
def filePath = "${getenv("HOME")}/Library/Application Support/IntelliJIdea13/code-history-mining/${projectName}-file-events.csv"
def events = new EventStorage(filePath).readAllEvents({}) { line, e -> println("Failed to parse line '${line}'") }

def allVisualizationsForGHPagesTemplate = template("all-visualizations-ghpages.html")
def visualization = createAllVisualizations(allVisualizationsForGHPagesTemplate)
def html = visualization.generate(new Context(events, projectName))

new File("${projectName}.html").write(html)