package history.events.csv4180
/**
 * CSV4180Reader provides a simple way to import CSV values from a file. The
 * reader extends BufferedReader for improved performance.
 *
 * @author Thomas Davis (sunsetbrew)
 * @copyright Copyright (c) 2010, Thomas Davis
 * @license http://opensource.org/licenses/mit-license.php MIT License
 */
public class CSVReader extends BufferedReader {

	/**
	 * Create a buffering character-input stream that uses a default-sized input
	 * buffer.
	 *
	 * @param in
	 *            A Reader
	 */
	public CSVReader(Reader reader) {
		super(reader);
	}

	/**
	 * Create a buffering character-input stream that uses an input buffer of
	 * the specified size.
	 *
	 * @param in
	 *            A Reader
	 * @param sz
	 *            Input-buffer size
	 */
	public CSVReader(Reader reader, int sz) {
		super(reader, sz);
	}

	/**
	 * Indicates if the last field read was at the end of the line.
	 *
	 * @return true if last field read was at the end of the line, false
	 *         otherwise or if no fields have been read.
	 */
	public boolean hasMoreFieldsOnLine() {
		return this.moreFieldsOnLine;
	}

	/**
	 * Indicates if all the data from the reader has been read.
	 *
	 * @return true if all the data from the reader has been read.
	 */
	public boolean isEOF() {
		return this.eof;
	}

	/**
	 * Reads the current line's fields into an ArrayList. This is a convenience
	 * method.
	 *
	 * @param fields
	 *            container for the fields in the current row. It will be
	 *            cleared.
	 * @throws IOException
	 *             on general I/O error
	 * @throws EOFException
	 *             on end of file
	 */
	public void readFields(List<String> fields) throws IOException {
		fields.clear();
		if (this.eof) {
			throw new EOFException();
		}
		fields.add(readField());
		while (this.moreFieldsOnLine) {
			fields.add(readField());
		}
	}

	/**
	 * Reads the next field from the input removing quotes as necessary.
	 *
	 * @return next field
	 * @throws IOException
	 *             If an I/O error occurs, may be an EOFException on end of
	 *             input.
	 */
	public String readField() throws IOException {
		int c;
		final int UNQUOTED = 0;
		final int QUOTED = 1;
		final int QUOTEDPLUS = 2;
		int state = UNQUOTED;

		if (this.eof) {
			throw new EOFException();
		}

		this.buffer.setLength(0);
		while ((c = this.read()) >= 0) {
			if (state == QUOTEDPLUS) {
				switch (c) {
					case '"':
						this.buffer.append('"');
						state = QUOTED;
						continue;
					default:
						state = UNQUOTED;
						break;
				}
			}
			if (state == QUOTED) {
				switch (c) {
					default:
						this.buffer.append((char) c);
						continue;
					case '"':
						state = QUOTEDPLUS;
						continue;
				}
			}

			// (state == UNQUOTED)
			switch (c) {
				case '"':
					state = QUOTED;
					continue;
				case '\r':
					continue;
				case '\n':
				case ',':
					this.moreFieldsOnLine = (c != '\n');
					return this.buffer.toString();
				default:
					this.buffer.append((char) c);
					continue;
			}

		}
		this.eof = true;
		this.moreFieldsOnLine = false;
		return this.buffer.toString();
	}

	/** @ignore */
	private boolean moreFieldsOnLine = true;

	/** @ignore */
	private boolean eof = false;

	/** @ignore */
	private final StringBuffer buffer = new StringBuffer();

}
