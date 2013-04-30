package history


class Measure {
	static Map<String, Long> durations = [:].withDefault{ 0 }

	static <T> T measure(String id, Closure<T> closure) {
		long start = System.currentTimeMillis()
		T result = closure()
		long time = System.currentTimeMillis() - start
		durations[id] += time
		result
	}

	static record(String id, long duration) {
		durations[id] += duration
	}
}
