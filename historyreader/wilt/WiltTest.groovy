package historyreader.wilt
import org.junit.Test

import static historyreader.wilt.Wilt.complexityByLineOf
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class WiltTest {
	private static int spacesInIndent = 2

	@Test void "calculate WILT for simple cases"() {
		assertThat(asString(complexityByLineOf("""
class Sample {
  Sample() {}
  Sample(int i) {}
}
    """, spacesInIndent)), equalTo("""
0:class Sample {
1:  Sample() {}
1:  Sample(int i) {}
0:}
"""))

		assertThat(asString(complexityByLineOf("""
class Sample {
  Sample() {
    int i = 123;
  }
  Sample(int i) {
    int j = i;
  }
}
    """, spacesInIndent)), equalTo("""
0:class Sample {
1:  Sample() {
2:    int i = 123;
1:  }
1:  Sample(int i) {
2:    int j = i;
1:  }
0:}
"""))
	}

	private static String asString(List indentDepthByLine) {
		"\n" + indentDepthByLine.collect{ it[0] + ":" + it[1] }.join("\n") + "\n"
	}
}

