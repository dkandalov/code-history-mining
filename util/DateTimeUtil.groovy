package util

import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat

class DateTimeUtil {
	static final TimeZone utc = TimeZone.getTimeZone("UTC")

	static Date date(String s) {
		new SimpleDateFormat("dd/MM/yyyy").with{
			timeZone = utc
			parse(s)
		}
	}

	static Date dateTime(String s) {
		exactDateTime(s)
	}

	static Date exactDateTime(String s) {
		def formats = [
				new SimpleDateFormat("kk:mm dd/MM/yyyy"),
				new SimpleDateFormat("kk:mm:ss dd/MM/yyyy"),
				new SimpleDateFormat("kk:mm:ss.SSS dd/MM/yyyy")
		]
		def result = formats.findResult{ DateFormat format ->
			try {
				format.timeZone = utc
				format.parse(s)
			} catch (ParseException ignored) {
				null
			}
		}
		if (result == null) throw new ParseException("Failed to parse string as dateTime: ${s}", -1)
		result
	}
}
