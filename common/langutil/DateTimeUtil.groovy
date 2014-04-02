package common.langutil

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

	static Date floorToDay(Date date, TimeZone timeZone = TimeZone.default) {
		oneDay.floor(date, timeZone)
	}

	static Date floorToWeek(Date date, TimeZone timeZone = TimeZone.default) {
		oneWeek.floor(date, timeZone)
	}

	static Date floorToMonth(Date date, TimeZone timeZone = TimeZone.default) {
		oneMonth.floor(date, timeZone)
	}

	static final oneDay = days(1)
	static final oneWeek = weeks(1)
	static final oneMonth = months(1)

	static interface TimeInterval {
		Date floor(Date date, TimeZone timeZone)
		Date next(Date date, TimeZone timeZone)
	}

	static TimeInterval days(int amount = 1) {
		new TimeInterval() {
			@Override Date floor(Date date, TimeZone timeZone = TimeZone.default) {
				Calendar.getInstance(timeZone).with{
					time = date
					set(MILLISECOND, 0)
					set(SECOND, 0)
					set(MINUTE, 0)
					set(HOUR_OF_DAY, 0)
					time
				}
			}

			@Override Date next(Date date, TimeZone timeZone = TimeZone.default) {
				Calendar.getInstance(timeZone).with{
					time = floor(date, timeZone)
					add(DAY_OF_MONTH, amount)
					time
				}
			}
		}
	}

	static TimeInterval weeks(int amount = 1) {
		new TimeInterval() {
			@Override Date floor(Date date, TimeZone timeZone = TimeZone.default) {
				Calendar.getInstance(timeZone).with{
					time = date
					set(MILLISECOND, 0)
					set(SECOND, 0)
					set(MINUTE, 0)
					set(HOUR_OF_DAY, 0)
					set(DAY_OF_WEEK, MONDAY)
					time
				}
			}

			@Override Date next(Date date, TimeZone timeZone = TimeZone.default) {
				Calendar.getInstance(timeZone).with{
					time = floor(date, timeZone)
					add(WEEK_OF_YEAR, amount)
					time
				}
			}
		}
	}

	static TimeInterval months(int amount = 1) {
		new TimeInterval() {
			@Override Date floor(Date date, TimeZone timeZone = TimeZone.default) {
				Calendar.getInstance(timeZone).with{
					time = date
					set(MILLISECOND, 0)
					set(SECOND, 0)
					set(MINUTE, 0)
					set(HOUR_OF_DAY, 0)
					set(DAY_OF_MONTH, 1)
					time
				}
			}

			@Override Date next(Date date, TimeZone timeZone = TimeZone.default) {
				Calendar.getInstance(timeZone).with{
					time = floor(date, timeZone)
					add(MONTH, amount)
					time
				}
			}
		}
	}

}
