package historystorage.csv4180

import java.util.regex.Matcher
import java.util.regex.Pattern
/**
 * Originally copied from https://csv4180.svn.sourceforge.net/svnroot/csv4180/
 * Should be compatible with http://www.apps.ietf.org/rfc/rfc4180.html
 *
 * @author Thomas Davis (sunsetbrew)
 * @copyright Copyright (c) 2010, Thomas Davis
 * @license http://opensource.org/licenses/mit-license.php MIT License
 */
class CSVWriter extends BufferedWriter {

	CSVWriter(Writer out) {
		super(out)
	}

	@Override void newLine() throws IOException {
		newLine = true
		super.newLine()
	}

	void writeField(String field) throws IOException {

		if (newLine) {
			newLine = false
		} else {
			write(',')
		}

		// case 0: empty string, simple :)
		if ((field == null) || (field.length() == 0)) {
			return
		}

		// case 1: field has quotes in it, if so convert to, quote field and
		// double all quotes
		Matcher matcher = escapePattern.matcher(field)
		if (matcher.find()) {
			write('"')
			tmpBuffer.setLength(0)
			matcher.appendReplacement(tmpBuffer, "\"\"")
			while (matcher.find()) {
				matcher.appendReplacement(tmpBuffer, "\"\"")
			}
			matcher.appendTail(tmpBuffer)
			write(tmpBuffer.toString())
			write('"')
			return
		}

		// case 2: field has a comma, carriage return or new line in it, if so
		// quote field and double all quotes
		matcher = specialCharsPattern.matcher(field)
		if (matcher.find()) {
			write('"')
			write(field)
			write('"')
			return
		}

		// case 3: safe string to just add
		append(field)
	}

	private boolean newLine = true
	private final StringBuffer tmpBuffer = new StringBuffer()
	private static Pattern escapePattern = Pattern.compile("(\")")
	private static Pattern specialCharsPattern = Pattern.compile("[,\r\n]")

}