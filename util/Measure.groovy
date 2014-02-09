package util


@SuppressWarnings("GrMethodMayBeStatic")
class Measure {
	private static Map<String, Long> durations = [:].withDefault{ 0 }

	def start() {
		reset()
	}

	static reset() {
		durations.clear()
	}

	def <T> T measure(String id, Closure<T> closure) {
		long start = System.currentTimeMillis()
		T result = closure()
		long time = System.currentTimeMillis() - start
		durations[id] += time
		result
	}

	def forEachDuration(Closure callback) {
		durations.entrySet().each{ callback(it) }
	}
}
