package historystorage.implementation
import groovy.transform.CompileStatic
/**
 * Originally copied from https://csv4180.svn.sourceforge.net/svnroot/csv4180/
 * Should be compatible with http://www.apps.ietf.org/rfc/rfc4180.html
 *
 * @author Thomas Davis (sunsetbrew)
 * @copyright Copyright (c) 2010, Thomas Davis
 * @license http://opensource.org/licenses/mit-license.php MIT License
 */
class CSVReader {
	private static final int UNQUOTED = 0
	private static final int QUOTED = 1
	private static final int QUOTEDPLUS = 2

	private String csvLine
	private final StringBuilder buffer = new StringBuilder()
	private boolean moreFieldsOnLine = true
	private int i

	@CompileStatic void readFields(String csvLine, List<String> fields) throws IOException {
		this.csvLine = csvLine
		fields.clear()
		i = 0

		fields.add(readField())
		while (moreFieldsOnLine) {
			fields.add(readField())
		}
	}

	@CompileStatic private String readField() throws IOException {
		int state = UNQUOTED

		buffer.setLength(0)

		while (i < csvLine.length()) {
			char c = csvLine.charAt(i++)

			if (state == QUOTEDPLUS) {
				if (c == '"') {
					buffer.append('"')
					state = QUOTED
				} else {
					return buffer.toString()
				}
			} else if (state == QUOTED) {
				if (c == '"') {
					state = QUOTEDPLUS
				} else {
					buffer.append(c)
				}
			} else { // (state == UNQUOTED)
				if (c == '"') {
					state = QUOTED
				} else if (c == '\r') {
				} else if (c == '\n' || c == ',') {
					moreFieldsOnLine = (c != '\n')
					return buffer.toString()
				} else {
					buffer.append((char) c)
				}
			}
		}
		moreFieldsOnLine = false

		buffer.toString()
	}
}
