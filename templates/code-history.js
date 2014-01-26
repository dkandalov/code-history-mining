function QuickFind(size) {
	this.size = size;
	this.connections = new Array(size);
	for (var i = 0; i < this.connections.length; i++) {
		this.connections[i] = i;
	}
}
QuickFind.prototype.areConnected = function(p1, p2) {
	return this.connections[p1] == this.connections[p2];
};
QuickFind.prototype.connect = function(p1, p2) {
	var p1Root = this.rootOf(this.connections[p1]);
	var p2Root = this.rootOf(this.connections[p2]);
	for (var i = 0; i < this.connections.length; i++) {
		if (this.connections[i] == p1Root) this.connections[i] = p2Root;
	}
};
QuickFind.prototype.rootOf = function(p) {
	return this.connections[p] == p ? p : this.rootOf(this.connections[p]);
};


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

// this is intended for projector in case default colors are too pale to see
function enableDarkColorsShortcut() {
	document.onkeydown = function(e) {
		e = window.event || e;
		if (String.fromCharCode(e.keyCode) == 'D') {
			d3.selectAll(".link")[0].forEach(function(link) {
				link.style["stroke"] = "#333000";
				link.style["stroke-opacity"] = 1.0;
			});
			d3.selectAll(".node")[0].forEach(function(node) {
				if (node.style["fill"] == "#1f77b4") node.style["fill"] = "#1033a2";
				else if (node.style["fill"] == "#aec7e8") node.style["fill"] = "#cc6666";
			});
		}
	};
}
enableDarkColorsShortcut();