package codemining.plugin

class CancelledException extends Exception {
	static check(indicator) {
		{ -> if (indicator.canceled) throw new CancelledException() }
	}
}
