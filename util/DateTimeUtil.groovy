package util

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
		new SimpleDateFormat("kk:mm dd/MM/yyyy").with{
			timeZone = utc
			parse(s)
		}
	}

	static Date exactDateTime(String s) {
		new SimpleDateFormat("kk:mm:ss dd/MM/yyyy").with{
			timeZone = utc
			parse(s)
		}
	}
}
