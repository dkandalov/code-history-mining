package analysis
import events.FileChangeEvent
import groovy.transform.Immutable

import static http.AllTemplates.getChangeSizeChartTemplate

@Immutable
class Visualization {
	final String name
	final Closure<String> generate

	static changeSizeChart = new Visualization("Change Size Chart", { Context context ->
		def json = Analysis.createJson_ChangeSize_Chart(context.events, context.checkIfCancelled)
		changeSizeChartTemplate.fillData(json).fillProjectName(context.projectName).text
	})

	static class Context {
		final List<FileChangeEvent> events
		final String projectName
		final Closure<Boolean> checkIfCancelled

		Context(List<FileChangeEvent> events, String projectName, Closure checkIfCancelled = {}) {
			this.events = events
			this.projectName = projectName
			this.checkIfCancelled = checkIfCancelled
		}
	}
}
