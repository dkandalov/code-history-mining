function movingAverage(data, timeInterval, getValue, period) {
	if (data.length < 2) return [];

	period = (period == null ? Math.round(data.length / 10) : period);
	if (period < 2) return [];

	var firstDate = data[0].date;
	var lastDatePlusOne = timeInterval.offset(data[data.length - 1].date, 1);
	var allDates = timeInterval.range(firstDate, lastDatePlusOne);
	if (allDates.length < period) return [];

	data = valuesForEachDate(data, allDates, getValue);

	var mean = d3.mean(allDates.slice(0, period), function(date){ return data[date]; });
	var result = [{date: allDates[period - 1], mean: mean}];

	for (var i = period; i < allDates.length; i++) {
		var date = allDates[i];
		var dateToExclude = allDates[i - period];
		mean += (data[date] - data[dateToExclude]) / period;
		result.push({date: date, mean: mean});
	}

	return result;
}

function valuesForEachDate(data, datesRange, getValue) {
	var result = {};
	datesRange.forEach(function(date) { result[date] = 0; });
	data.forEach(function(d) { result[d.date] = getValue(d); });
	return result;
}
