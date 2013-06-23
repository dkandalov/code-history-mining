package util

import org.jetbrains.annotations.Nullable

class CancelledException extends Exception {
	static Closure watching(@Nullable indicator, Closure closure = {}) {
		{ arg1 = null, arg2 = null->
			if (indicator?.canceled) throw new CancelledException()

			if (arg1 != null && arg2 != null) closure(arg1, arg2)
			else if (arg1 != null && arg2 == null) closure(arg1)
			else closure()
		}
	}
}
