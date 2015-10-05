package codehistoryminer.plugin;

import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NonNls;

import java.io.PrintWriter;
import java.io.StringWriter;

public class Unscramble {
    public static String unscrambleThrowable(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return Unscramble.normalizeText(writer.getBuffer().toString());
    }

    public static String normalizeText(@NonNls String text) {
        StringBuilder builder = new StringBuilder(text.length());

        text = text.replaceAll("(\\S[ \\t\\x0B\\f\\r]+)(at\\s+)", "$1\n$2");
        String[] lines = text.split("\n");

        boolean first = true;
        boolean inAuxInfo = false;
        for (String line : lines) {
            //noinspection HardCodedStringLiteral
            if (!inAuxInfo && (line.startsWith("JNI global references") || line.trim().equals("Heap"))) {
                builder.append("\n");
                inAuxInfo = true;
            }
            if (inAuxInfo) {
                builder.append(trimSuffix(line)).append("\n");
                continue;
            }
            if (!first && mustHaveNewLineBefore(line)) {
                builder.append("\n");
                if (line.startsWith("\"")) builder.append("\n"); // Additional line break for thread names
            }
            first = false;
            int i = builder.lastIndexOf("\n");
            CharSequence lastLine = i == -1 ? builder : builder.subSequence(i + 1, builder.length());
            if (lastLine.toString().matches("\\s*at") && !line.matches("\\s+.*")) builder.append(" "); // separate 'at' from file name
            builder.append(trimSuffix(line));
        }
        return builder.toString();
    }

    @SuppressWarnings("RedundantIfStatement")
    private static boolean mustHaveNewLineBefore(String line) {
        final int nonWs = CharArrayUtil.shiftForward(line, 0, " \t");
        if (nonWs < line.length()) {
            line = line.substring(nonWs);
        }

        if (line.startsWith("at")) return true;        // Start of the new stack frame entry
        if (line.startsWith("Caused")) return true;    // Caused by message
        if (line.startsWith("- locked")) return true;  // "Locked a monitor" logging
        if (line.startsWith("- waiting")) return true; // "Waiting for monitor" logging
        if (line.startsWith("- parking to wait")) return true;
        if (line.startsWith("java.lang.Thread.State")) return true;
        if (line.startsWith("\"")) return true;        // Start of the new thread (thread name)

        return false;
    }

    private static String trimSuffix(final String line) {
        int len = line.length();

        while ((0 < len) && (line.charAt(len-1) <= ' ')) {
            len--;
        }
        return (len < line.length()) ? line.substring(0, len) : line;
    }
}
