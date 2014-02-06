package util

import org.junit.Test

import static util.FileUtil.commonAncestorOf

class FileUtilTest {
	@Test void "should find common ancestor of list of paths"() {
		assert commonAncestorOf([]) == ""
		assert commonAncestorOf(["/a/b/c"]) == "/a/b/c"
		assert commonAncestorOf(["/a/b/c", "/a/b/d"]) == "/a/b"
		assert commonAncestorOf(["/a/b/c", "/a/b/d", "/a/x/y"]) == "/a"
		assert commonAncestorOf(["/a/b/c", "/x/b/z"]) == ""
	}
}
