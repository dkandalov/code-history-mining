package events.csv4180

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

	private final Reader reader
	private final StringBuffer buffer = new StringBuffer()
	private boolean moreFieldsOnLine = true
	private boolean eof = false

	CSVReader(Reader reader) {
		this.reader = reader
	}

	@CompileStatic void readFields(List<String> fields) throws IOException {
		fields.clear()
		if (eof) {
			throw new EOFException()
		}
		fields.add(readField())
		while (moreFieldsOnLine) {
			fields.add(readField())
		}
	}

	@CompileStatic private String readField() throws IOException {
		int state = UNQUOTED

		if (eof) {
			throw new EOFException()
		}

		buffer.setLength(0)

		int i
		while ((i = reader.read()) >= 0) {
			char c = (char) i
			if (state == QUOTEDPLUS) {
				switch (c) {
					case '"':
						buffer.append('"')
						state = QUOTED
						continue
					default:
						state = UNQUOTED
						break
				}
			}
			if (state == QUOTED) {
				switch (c) {
					default:
						buffer.append((char) c)
						continue
					case '"':
						state = QUOTEDPLUS
						continue
				}
			}

			// (state == UNQUOTED)
			switch (c) {
				case '"':
					state = QUOTED
					continue
				case '\r':
					continue
				case '\n':
				case ',':
					moreFieldsOnLine = (c != '\n')
					return buffer.toString()
				default:
					buffer.append((char) c)
					continue
			}

		}
		eof = true
		moreFieldsOnLine = false

		buffer.toString()
	}
}
