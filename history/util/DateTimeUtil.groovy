package history.util

import java.text.SimpleDateFormat

class DateTimeUtil {
	static Date date(String s) {
		new SimpleDateFormat("dd/MM/yyyy").parse(s)
	}

	static Date dateTime(String s) {
		new SimpleDateFormat("kk:mm dd/MM/yyyy").parse(s)
	}

	static Date exactDateTime(String s) {
		new SimpleDateFormat("kk:mm:ss dd/MM/yyyy").parse(s)
	}
}
