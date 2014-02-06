package util

class FileUtil {
	static String commonAncestorOf(List<String> paths) {
		if (paths.size() > 1) commonAncestorOf(paths.first(), commonAncestorOf(paths.tail()))
		else if (paths.size() == 1) paths.first()
		else ""
	}

	static String commonAncestorOf(String file1, String file2) {
		String[] path1 = file1.split("/")
		String[] path2 = file2.split("/")

		int lastEqualIndex = -1
		for (int i = 0; i < path1.length && i < path2.length; i++) {
			if (path1[i] == path2[i]) {
				lastEqualIndex = i
			} else {
				break
			}
		}
		lastEqualIndex == -1 ? "" : path1.take(lastEqualIndex + 1).join("/")
	}
}
