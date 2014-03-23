package util


class Measure {
	private final Map<String, Long> durations = [:].withDefault{ 0 }

	def synchronized start() {
		reset()
	}

	def synchronized reset() {
		durations.clear()
	}

	def synchronized <T> T measure(String id, Closure<T> closure) {
		long start = System.currentTimeMillis()
		T result = closure()
		long time = System.currentTimeMillis() - start
		durations[id] += time
		result
	}

	def synchronized forEachDuration(Closure callback) {
		durations.entrySet().each{ callback(it) }
	}
}
