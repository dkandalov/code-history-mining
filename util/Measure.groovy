package util


class Measure {
	static Map<String, Long> durations = [:].withDefault{ 0 }

	static <T> T measure(String id, Closure<T> closure) {
		long start = System.currentTimeMillis()
		T result = closure()
		long time = System.currentTimeMillis() - start
		durations[id] += time
		result
	}

	static forEachDuration(Closure callback) {
		durations.entrySet().each{ callback(it.key + ": " + it.value) }
	}
}
