import com.intellij.openapi.diff.impl.fragments.LineFragment
import com.intellij.openapi.diff.impl.util.TextDiffTypeEnum

import static com.intellij.openapi.diff.impl.ComparisonPolicy.*
import com.intellij.openapi.diff.impl.processing.TextCompareProcessor
import static com.intellij.openapi.diff.impl.util.TextDiffTypeEnum.*


import static intellijeval.PluginUtil.*

if (isIdeStartup) return

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
  }

} catch (AssertionError assertionError) {
  def writer = new StringWriter()
  assertionError.printStackTrace(new PrintWriter(writer))
  showInConsole(writer.buffer.toString(), "TextCompareProcessorTest", project)
}
showInConsole("OK...", "TextCompareProcessorTest", project)


def assertFragment(LineFragment fragment, TextDiffTypeEnum diffType, leftRange, rightRange) {
  fragment.with {
    assert type == diffType
    assert [startingLine1, endLine1] == leftRange
    assert [startingLine2, endLine2] == rightRange
  }
}