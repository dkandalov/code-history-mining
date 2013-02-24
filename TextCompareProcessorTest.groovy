import com.intellij.openapi.diff.impl.fragments.LineFragment
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.diff.impl.util.TextDiffTypeEnum
import com.intellij.openapi.project.Project

import static com.intellij.openapi.diff.impl.ComparisonPolicy.IGNORE_SPACE
import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
import static intellijeval.PluginUtil.showInConsole

// Can only be executed within IntelliJEval.
// E.g. like this
//   TextCompareProcessorTest.testTextCompare(project)
//   if (true)return
// Couldn't make it a proper unit test because of CommonBundle.message invocations in IntelliJ code :(
class TextCompareProcessorTest {

	static void testTextCompare(Project project) {
		try {
			new TextCompareProcessor(IGNORE_SPACE).with {
				process("", "").with {
					assert size() == 0
				}

				// single-line diffs
				process("a", "").with {
					assert size() == 1
					assertFragment(first(), DELETED, [0, 1], [0, 0])
				}
				process("", "a").with {
					assert size() == 1
					assertFragment(first(), INSERT, [0, 0], [0, 1])
				}
				process("a", "a").with {
					assert size() == 1
					assertFragment(first(), null, [0, 1], [0, 1])
				}
				process("abc", "ac").with {
					assert size() == 1
					assertFragment(first(), CHANGED, [0, 1], [0, 1])
				}
				process("ac", "abc").with {
					assert size() == 1
					assertFragment(first(), CHANGED, [0, 1], [0, 1])
				}
				process("abc", "bc").with {
					assert size() == 1
					assertFragment(first(), CHANGED, [0, 1], [0, 1])
				}

				// two-line diffs
				process("abc\ndef", "abc\ndef").with {
					assert size() == 1
					assertFragment(first(), null, [0, 2], [0, 2])
				}
				process("abc\ndef", "abc").with {
					assert size() == 2
					assertFragment(get(0), null, [0, 1], [0, 1])
					assertFragment(get(1), DELETED, [1, 2], [1, 1])
				}
				process("abc", "abc\ndef").with {
					assert size() == 2
					assertFragment(get(0), null, [0, 1], [0, 1])
					assertFragment(get(1), INSERT, [1, 1], [1, 2])
				}
				process("abc\ndf", "abc\ndef").with {
					assert size() == 2
					assertFragment(get(0), null, [0, 1], [0, 1])
					assertFragment(get(1), CHANGED, [1, 2], [1, 2])
				}
				process("abc\ndef", "abc\ndf").with {
					assert size() == 2
					assertFragment(get(0), null, [0, 1], [0, 1])
					assertFragment(get(1), CHANGED, [1, 2], [1, 2])
				}

				// three-line diffs
				process("abc\ndef\nghi", "abc\ndef\nghi").with {
					assert size() == 1
					assertFragment(get(0), null, [0, 3], [0, 3])
				}
				process("abc\ndef\nghi", "abc\ndef").with {
					assert size() == 2
					assertFragment(get(0), null, [0, 2], [0, 2])
					assertFragment(get(1), DELETED, [2, 3], [2, 2])
				}
				process("abc\ndef\nghi", "abc\nghi").with {
					assert size() == 3
					assertFragment(get(0), null, [0, 1], [0, 1])
					assertFragment(get(1), DELETED, [1, 2], [1, 1])
					assertFragment(get(2), null, [2, 3], [1, 2])
				}
				process("abc\ndef\nghi", "def\nghi").with {
					assert size() == 2
					assertFragment(get(0), DELETED, [0, 1], [0, 0])
					assertFragment(get(1), null, [1, 3], [0, 2])
				}
				process("abc\ndef\nghi", "abc\nghi\ndef").with {
					assert size() == 4
					assertFragment(get(0), null, [0, 1], [0, 1])
					assertFragment(get(1), DELETED, [1, 2], [1, 1])
					assertFragment(get(2), null, [2, 3], [1, 2])
					assertFragment(get(3), INSERT, [3, 3], [2, 3])
				}
			}

			showInConsole("OK...", "TextCompareProcessorTest", project)

		} catch (AssertionError assertionError) {
			def writer = new StringWriter()
			assertionError.printStackTrace(new PrintWriter(writer))
			showInConsole(writer.buffer.toString(), "TextCompareProcessorTest", project)
		}
	}

	static assertFragment(LineFragment fragment, TextDiffTypeEnum diffType, leftRange, rightRange) {
		fragment.with {
			assert type == diffType
			assert [startingLine1, endLine1] == leftRange
			assert [startingLine2, endLine2] == rightRange
		}
	}
}

