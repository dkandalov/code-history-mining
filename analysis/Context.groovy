package analysis

import common.events.FileChangeEvent

class Context {
	final List<FileChangeEvent> events
	final String projectName
	final Closure<Boolean> checkIfCancelled

	Context(List<FileChangeEvent> events, String projectName, Closure checkIfCancelled = {}) {
		this.events = events
		this.projectName = projectName
		this.checkIfCancelled = checkIfCancelled
	}
}
