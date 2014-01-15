package historyreader
import com.intellij.openapi.diff.impl.fragments.LineFragment
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import com.intellij.openapi.diff.impl.util.TextDiffTypeEnum
import com.intellij.openapi.project.Project
import org.junit.Test

import static com.intellij.openapi.diff.impl.ComparisonPolicy.IGNORE_SPACE
import static com.intellij.openapi.diff.impl.ComparisonPolicy.TRIM_SPACE
import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*
/**
 * This test exists to explore how {@link TextCompareProcessor} compares text.
 *
 * Can only be executed inside LivePlugin (https://github.com/dkandalov/live-plugin).
 * (Couldn't make it a proper unit test because of CommonBundle.message invocations in IntelliJ code)
 */
class TextCompareProcessorTest {
	@Test def "basic text comparisons"() {
		new TextCompareProcessor(TRIM_SPACE).with {
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
	}

	@Test "differences between IGNORE_SPACE and TRIM_SPACE comparison"() {
		new TextCompareProcessor(IGNORE_SPACE).with {
			process("some expression", "some  expression").with {
				assert size() == 1
				assertFragment(first(), null, [0, 1], [0, 1])
				assert first().childrenIterator == null
			}
			process("(some) expression", "(some)  expression").with {
				assert size() == 1
				assertFragment(first(), null, [0, 1], [0, 1])
				assert first().childrenIterator == null
			}
		}
		new TextCompareProcessor(TRIM_SPACE).with {
			process("some expression", "some  expression").with {
				assert size() == 1
				assertFragment(first(), CHANGED, [0, 1], [0, 1])
				assert first().childrenIterator == null
			}
			process("(some) expression", "(some)  expression").with {
				assert size() == 1
				assertFragment(first(), INSERT, [0, 1], [0, 1])
				assert first().childrenIterator != null
				first().childrenIterator.toList().with {
					assert it[0].type == null
					assert it[1].type == INSERT
					assert it[2].type == null
				}
			}
			process("(some)  expression", "(some) expression").with {
				assert size() == 1
				assertFragment(first(), DELETED, [0, 1], [0, 1])
				first().childrenIterator.toList().with {
					assert it[0].type == null
					assert it[1].type == DELETED
					assert it[2].type == null
				}
			}
		}
	}

	private static assertFragment(LineFragment fragment, TextDiffTypeEnum diffType, leftRange, rightRange) {
		fragment.with {
			assert type == diffType
			assert [startingLine1, endLine1] == leftRange
			assert [startingLine2, endLine2] == rightRange
		}
	}

	TextCompareProcessorTest(Map context) {
		this.project = context.project
	}

	private final Project project
}

