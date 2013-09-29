package util

import org.jetbrains.annotations.Nullable

class CancelledException extends Exception {
	static check(@Nullable indicator) {
		if (indicator?.canceled) throw new CancelledException()
	}
}
