function newEmptyBarChartLabel(root, svgRoot, uiConfig) {
	var label = root.append("div")
		.html("Unfortunately, there is nothing to show.")
		.style("position", "absolute")
		.style("opacity", 0)
		.attr("class", "tooltip");
	var it = {};
	it.update = function(update) {
		if (update.maxY > 0) {
			label.style("opacity", 0);
		} else {
			label.style("opacity", 0.9)
				.style("top", function(){
					return offsetTop(svgRoot) + uiConfig.margin.top + (uiConfig.height / 2) - (this.offsetHeight / 2) + "px";
				})
				.style("left", function(){
					return offsetLeft(svgRoot) + uiConfig.margin.left + (uiConfig.width / 2) - (this.offsetWidth / 2) + "px";
				});
		}
	};
	return it;
}

function newBarShading() {
	var it = {};
	it.update = function(update) {
		if (update.event === "mouseout") {
			update.bar.removeAttribute("style");
		} else {
			update.bar.style.fill = shadeColor(update.bar.parentNode.style.fill, -0.15);
		}
	};
	return it;
}

function newBarTooltip(root, svgRoot, uiConfig, settings) {
	if (settings === undefined) settings = {};
	if (settings.showCategory === undefined) settings.showCategory = true;
	if (settings.css === undefined) settings.css = "";
	if (settings.delay === undefined) settings.delay = 1500;
	if (settings.delayBeforeHide === undefined) settings.delayBeforeHide = 100;

	var div = root.append("div")
		.attr("class", settings.css)
		.style("position", "absolute")
		.style("opacity", 0);

	var dateFormat = d3.time.format("%d/%m/%Y");
	var valueFormat = function(n) {
		var s = d3.format(",")(n);
		return s.length < 6 ? s : d3.format("s")(n);
	};
	var lastUpdate = null;

	var it = {};
	it.update = function(update) {
		lastUpdate = update;
		if (update.event === "mouseout") {
			// use timeout because when moving between two elements, onMouseOver event for next element
			// can come before onMouseOut for old one, what results in tooltip disappearing
			setTimeout(function() {
				if (lastUpdate.event === "mouseout")
					div.transition().delay(0).style("opacity", 0);
			}, settings.delayBeforeHide);
		} else {
			var x = parseInt(update.bar.getAttribute("x"));
			var y = parseInt(update.bar.getAttribute("y"));
			var width = parseInt(update.bar.getAttribute("width"));
			var height = parseInt(update.bar.getAttribute("height"));

			var left = offsetLeft(svgRoot) + uiConfig.margin.left + x + width + 5;
			if (left > offsetLeft(svgRoot) + uiConfig.margin.left + uiConfig.width) {
				left = offsetLeft(svgRoot) + uiConfig.margin.left + uiConfig.width;
			}
			var top = offsetTop(svgRoot) + uiConfig.margin.top + y + (height / 2);

			var text = ["Value: " + valueFormat(update.value), "Date: " + dateFormat(update.date)];
			if (settings.showCategory) text.push("Category: " + update.category);
			div.html(text.join("</br>"))
				.style("left", left + "px")
				.style("top", top + "px");

			// can determine actual height only after adding text
			var actualHeight = offsetHeight(div);
			top = offsetTop(svgRoot) + uiConfig.margin.top + y + (height / 2 - actualHeight / 2);
			div.style("top",  top + "px");

			div.transition().delay(settings.delay).style("opacity", 0.9);
		}
	};
	return it;
}

// based on https://gist.github.com/ZJONSSON/3918369
function newLegend(root, position) {
	var it = {};
	it.update = function(items) {
		// always redraw legend so that it would be above graph
		root.select(".legend").remove();
		var legend = root.append("g").attr("class", "legend")
			.attr("transform", "translate(" + position.x + "," + position.y + ")");

		var box = legend.append("rect").attr("class", "box");
		var itemList = legend.append("g").attr("class", "items");

		itemList.selectAll("text")
			.data(items)
			.call(function(d) { d.enter().append("text") })
			.attr("y", function(d, i) { return i * 1.04 + "em"; })
			.attr("x", "1em")
			.text(function(d) { return d.category; });

		itemList.selectAll("circle")
			.data(items)
			.call(function(d) { d.enter().append("circle") })
			.attr("cy", function(d, i) { return i - 0.4 + "em"; })
			.attr("cx", 0)
			.attr("r", "0.4em")
			.style("fill",function(d) { return d.color; });

		var legendPadding = 5;
		var itemListBox = itemList[0][0].getBBox();
		box.attr("x", itemListBox.x - legendPadding)
			.attr("y", itemListBox.y - legendPadding)
			.attr("height", itemListBox.height + 2 * legendPadding)
			.attr("width", itemListBox.width + 2 * legendPadding);
	};
	return it;
}


function newGroupByDropDown(root, stackedData, label, groupByNames) {
	return newDropDown(root, label, groupByNames,
		function(update) {
			return update.groupByIndex;
		},
		function(newValue) {
			stackedData.groupBy(parseInt(newValue));
		}
	);
}

function newGroupIndexDropDown(root, stackedData, label, groupNames) {
	return newDropDown(root, label, groupNames,
		function(update) {
			return update.groupIndex;
		},
		function(newValue) {
			stackedData.setGroupIndex(newValue);
		}
	);
}


function newXBrush(root, uiConfig, xScale, height, yPosition, color) {
	height = height === undefined ? 50 : height;
	yPosition = yPosition === undefined ? (uiConfig.height + uiConfig.margin.top) : yPosition;

	var brushUiConfig = extendCopyOf(uiConfig, {height: height});
	var brushXScale = newXScale(uiConfig);
	var brushYScale = newYScale(brushUiConfig);
	var brush = d3.svg.brush().x(brushXScale);

	var g = root.append("g").attr("transform", "translate(0" + "," + yPosition + ")");

	brush.update = function(update) {
		brushXScale.update(update);
		brushYScale.update(update);
		updateXScale();
		updateUI(update);
	};
	brush.on("brush", function() {
		updateXScale();
	});
	return brush;


	function updateUI(update) {
		g.selectAll("g").remove();
		g.selectAll("defs").remove();

		var bars = newBars(g, brushUiConfig, brushXScale, brushYScale, "brushBars", color);
		if (update !== null) bars.update(update);

		g.append("g")
			.attr("class", "x brush")
			.call(brush)
			.selectAll("rect")
			.attr("height", height);
	}
	function updateXScale() {
		var extent = brush.empty() ? brushXScale.domain() : brush.extent();
		xScale.setDomain(extent);
	}
}

function newBars(root, uiConfig, xScale, yScale, idPostfix, color) {
	idPostfix = (idPostfix === undefined ? "-bars" : "-" + idPostfix);
	color = (color === undefined ? d3.scale.category20() : color);
	var dataStacked;

	root.append("defs").append("clipPath").attr("id", "clip" + idPostfix)
		.append("rect").attr("width", uiConfig.width).attr("height", uiConfig.height).attr("x", 1);

	var it = {};
	var notifyCategoryListeners = observable(it);
	var notifyHoverListeners = observable(it, "onHover");
	it.update = function(update) {
		dataStacked = update.dataStacked;

		root.selectAll(".layer" + idPostfix).remove();

		var layer = root.selectAll(".layer" + idPostfix)
			.data(update.dataStacked)
			.enter().append("g")
			.attr("class", "layer" + idPostfix)
			.style("fill", function(d, i) { return color(i); });

		var rect = layer.selectAll("rect")
			.data(function(d) { return d; })
			.enter().append("rect")
			.attr("clip-path", "url(#clip" + idPostfix + ")")
			.attr("x", function(d) { return xScale(d.x); })
			.attr("y", function(d) { return yScale(d.y0 + d.y); })
			.attr("width", barWidth())
			.attr("height", function(d) { return yScale(d.y0) - yScale(d.y0 + d.y); });

		rect.on("mouseover", function(d) {
			notifyHoverListeners({event: "mouseover", bar: this, value: d.y, date: d.x, category: getCategory(d)});
		}).on("mouseout", function() {
			notifyHoverListeners({event: "mouseout", bar: this});
		});

		var categoryUpdate = [];
		update.dataStacked.forEach(function(it, i) {
			categoryUpdate.push({ category: getCategory(it[0]), color: color(i) });
		});
		notifyCategoryListeners(categoryUpdate);
	};
	it.onXScaleUpdate = function(updatedXScale) {
		xScale = updatedXScale;

		var layer = root.selectAll(".layer" + idPostfix).data(dataStacked);
		layer.selectAll("rect")
			.data(function(d) { return d; })
			.attr("x", function(d) { return xScale(d.x); })
			.attr("width", barWidth());
	};
	return it;

	function getCategory(d) {
		return d["category"];
	}
	function nonZero(length) {
		return (length > 0 ? length : 0.0000001);
	}
	function barWidth() {
		var barWidth = uiConfig.width / nonZero(xScale.amountOfValues) - 1;
		barWidth = Math.floor(barWidth);
		if (barWidth > 20) barWidth = barWidth - 1;
		if (barWidth < 1) barWidth = 1;
		return barWidth;
	}
}


function newYAxis(root, label, y) {
	var valueFormat = function(n) {
		var s = d3.format(",")(n);
		return s.length < 6 ? s : d3.format("s")(n);
	};
	var axis = d3.svg.axis().scale(y).orient("left").tickFormat(valueFormat);
	axis.update = function() {
		root.selectAll(".y.axis").remove();
		root.append("g")
			.attr("class", "y axis")
			.call(axis)
			.append("text")
			.attr("transform", "rotate(-90)")
			.attr("y", 6)
			.attr("dy", ".71em")
			.style("text-anchor", "end")
			.text(label);
	};
	return axis;
}

function newXAxis(root, uiConfig, xScale) {
	var axis = d3.svg.axis().scale(xScale).orient("bottom");
	axis.update = function() {
		root.selectAll(".x.axis").remove();
		root.append("g")
			.attr("class", "x axis")
			.attr("transform", "translate(0," + uiConfig.height + ")")
			.call(axis);
	};
	axis.onXScaleUpdate = function() {
		root.select(".x.axis").call(axis);
	};
	return axis;
}


function newXScale(uiConfig) {
	var timeInterval = d3.time.day;
	var x = d3.time.scale().nice().rangeRound([0, uiConfig.width]);
	x.update = function(update) {
		timeInterval = update.dataTimeInterval;
		x.domain([update.minX, timeInterval.offset(update.maxX, 1)]);
		x.amountOfValues = amountOfValuesIn(x.domain(), timeInterval);
	};
	var notifyListeners = observable(x);
	x.setDomain = function(extent) {
		x.amountOfValues = amountOfValuesIn(extent, timeInterval);
		x.domain(extent);
		notifyListeners(x);
	};
	return x;

	function amountOfValuesIn(domain, timeInterval) {
		var count = 0;
		var from = domain[0];
		var to = domain[1];
		while (from < to) {
			count++;
			from = timeInterval.offset(from, 1)
		}

		var millisInOneInterval = timeInterval.offset(from, 1) - from;
		var remainder = (to - from) / millisInOneInterval;

		return count + remainder;
	}
}

function newYScale(uiConfig) {
	var y = d3.scale.linear().range([uiConfig.height, 0]);
	y.update = function(update) {
		y.domain([update.minY, update.maxY]);
	};
	return y;
}

function autoGroup(data) {
	var ranOnce = false;
	var it = {};
	it.update = function(update) {
		if (ranOnce) return;
		ranOnce = true;

		var timeInterval = bestTimeIntervalForGrouping(update.minX, update.maxX);
		data.groupBy(timeInterval);
	};
	return it;

	function bestTimeIntervalForGrouping(minTime, maxTime) {
		if (new Date(maxTime - minTime) < d3.time.month.offset(new Date(0), 1)) {
			return d3.time.day;
		} else if (new Date(maxTime - minTime) < d3.time.month.offset(new Date(0), 24)) {
			return d3.time.monday;
		} else {
			return d3.time.month;
		}
	}
}


function stackedData(rawCsv) {
	var groupedData = groupByCategory(d3.csv.parse(rawCsv.trim()));
	var dataStacked = groupedData.length === 0 ? [] : d3.layout.stack()(groupedData);

	var it = {};
	var notifyListeners = observable(it);
	it.sendUpdate = function() {
		notifyListeners({
			dataStacked: dataStacked
		});
	};
	return it;

	function getCategory(d) {
		return d["category"];
	}
	function groupByCategory(data) {
		var dateFormat = d3.time.format("%d/%m/%Y");
		return d3.nest().key(function(d){ return getCategory(d); }).entries(data)
			.map(function (entry) {
				return entry.values.map(function (d) {
					return {
						x: dateFormat.parse(d.date),
						y: parseFloat(d["value"]),
						category: getCategory(d)
					};
				});
			});
	}
}

function withMinMax(data) {
	var dataUpdate;
	var it = _.clone(data);
	var notifyListeners = observable(it);
	data.onUpdate(function(update) {
		dataUpdate = update;
	});
	it.sendUpdate = function() {
		data.sendUpdate();
		var minX = d3.min(dataUpdate.dataStacked, function(d) {
			return d3.min(d, function(dd) {
				return dd.x;
			});
		});
		var maxX = d3.max(dataUpdate.dataStacked, function(d) {
			return d3.max(d, function(dd) {
				return dd.x;
			});
		});
		var minY = 0;
		var maxY = d3.max(dataUpdate.dataStacked, function(layer) {
			return d3.max(layer, function(d) {
				return d.y0 + d.y;
			});
		});
		notifyListeners(extendCopyOf(dataUpdate, {
			minX: minX,
			maxX: maxX,
			minY: minY,
			maxY: maxY
		}));
	};
	return it;
}

function groupedByTime(data) {
	var timeIntervals = [d3.time.day, d3.time.monday, d3.time.month];
	var groupByIndex = 0;
	var groupedData;
	var dataUpdate;

	var it = _.clone(data);
	var notifyListeners = observable(it);
	data.onUpdate(function(update) {
		dataUpdate = update;
	});
	it.sendUpdate = function() {
		data.sendUpdate();
		regroupStackedData();
		notifyListeners(extendCopyOf(dataUpdate, {
			dataStacked: groupedData,
			groupByIndex: groupByIndex,
			dataTimeInterval: timeIntervals[groupByIndex]
		}));
	};
	it.groupBy = function(value) {
		if (!_.isNumber(value)) {
			var i = timeIntervals.indexOf(value);
			if (i === -1) return;
			value = i;
		}
		if (value === groupByIndex) return;

		groupByIndex = value;
		it.sendUpdate();
	};
	return it;

	function getCategory(d) {
		return d["category"];
	}
	function groupBy(timeInterval, daysData) {
		if (timeInterval === d3.time.day) return daysData;
		return d3.values(d3.nest()
			.key(function(d) { return timeInterval.floor(d.x); })
			.rollup(function(days) {
				var sumOfY = d3.sum(days, function (d) { return d.y; });
				var sumOfY0 = d3.sum(days, function (d) { return d.y0; });
				var date = timeInterval.floor(days[0].x);
				return { category: getCategory(days[0]), x: date, y: sumOfY, y0: sumOfY0 };
			})
			.map(daysData));
	}
	function regroupStackedData() {
		groupedData = dataUpdate.dataStacked.map(function(it) {
			return groupBy(timeIntervals[groupByIndex], it);
		});
	}
}

function filteredByPercentile(data) {
	var percentile = 1.0;
	var dataUpdate;
	var it = _.clone(data);
	var notifyListeners = observable(it);
	data.onUpdate(function(update) {
		dataUpdate = update;
	});
	it.sendUpdate = function() {
		data.sendUpdate();
		notifyListeners(extendCopyOf(dataUpdate, {
			percentile: percentile,
			dataStacked: filter(dataUpdate.dataStacked, percentile)
		}));
	};
	it.setPercentile = function(value) {
		percentile = value;
		it.sendUpdate();
	};
	return it;

	function filter(dataStacked, percentile) {
		if (percentile === 1.0) return dataStacked;
		var amountOfCategories = dataStacked.length;
		var amountOfDataPoints = d3.max(dataStacked, function(it) { return it.length; });

		var dataWithTotal = [];
		for (var i = 0; i < amountOfDataPoints; i++) {
			var total = 0;
			for (var categoryIndex = 0; categoryIndex < amountOfCategories; categoryIndex++) {
				total += dataStacked[categoryIndex][i].y;
			}
			dataWithTotal.push({ index: i, total: total });
		}
		var sortedData = dataWithTotal.sort(function (a, b){ return a.total - b.total; });
		var n = sortedData[Math.round((sortedData.length - 1) * percentile)];

		var indicesToExclude = _.unique(sortedData
			.filter(function(it) { return it.total > n.total; })
			.map(function(it) { return it.index; }));

		return dataStacked.map(function(array) {
			var result = [];
			for (var i = 0; i < array.length; i++) {
				if (!_.contains(indicesToExclude, i)) result.push(array[i]);
			}
			return result;
		});
	}
}

function newStackedData(rawCsv) {
	return withMinMax(filteredByPercentile(groupedByTime(stackedData(rawCsv))));
}

function delegateByIndexTo(stackedData, method, fromData, getIndex) {
	fromData[method] = function(value) {
		for (var i = 0; i < stackedData.length; i++) {
			if (i !== getIndex()) stackedData[i][method](value); // skip selected index to make it send update last
		}
		stackedData[getIndex()][method](value);
		fromData.sendUpdate();
	};
}

function newMultipleStackedData(rawCsvArray) {
	var groupIndex = 0;
	var stackedData = rawCsvArray.map(function(it) { return newStackedData(it); });

	var it = {};
	var notifyListeners = observable(it);
	it.sendUpdate = function() {
		stackedData[groupIndex].sendUpdate();
	};
	it.setGroupIndex = function(value) {
		groupIndex = value;
		it.sendUpdate();
	};
	delegateByIndexTo(stackedData, "groupBy", it, function() { return groupIndex; });
	delegateByIndexTo(stackedData, "setPercentile", it, function() { return groupIndex; });
	stackedData.forEach(function(it) {
		it.onUpdate(function(update) {
			update.groupIndex = groupIndex;
			notifyListeners(update);
		});
	});
	return it;
}

function newMultipleStackedDataWithTimeIntervals(rawCsvArray, timeIntervals) {
	var groupIndex = 0;
	var stackedData = rawCsvArray.map(function(it) { return newStackedData(it); });

	var it = {};
	var notifyListeners = observable(it);
	it.sendUpdate = function() {
		stackedData[groupIndex].sendUpdate();
	};
	it.setGroupIndex = function(value) {
		groupIndex = value;
		it.sendUpdate();
	};
	it.groupBy = function(value) {
		var i = timeIntervals.indexOf(value);
		if (i !== -1) it.setGroupIndex(i);
	};
	delegateByIndexTo(stackedData, "setPercentile", it, function() { return groupIndex; });
	stackedData.forEach(function(it) {
		it.onUpdate(function(update) {
			update.groupIndex = groupIndex;
			update.dataTimeInterval = timeIntervals[groupIndex];
			notifyListeners(update);
		});
	});
	return it;
}


function newShowMovingAverageCheckBox(root, movingAverageLine) {
	root.append("label").html("Moving average: ");
	root.append("input").attr("type", "checkbox").on("click", function() {
		movingAverageLine.setVisible(this.checked);
	});
}

function newMovingAverageLine(root, uiConfig, xScale, yScale, idPostfix) {
	idPostfix = (idPostfix === undefined ? "-movingAvg" : "-" + idPostfix);
	var line = d3.svg.line()
		.x(function(d) { return xScale(d.date); })
		.y(function(d) { return yScale(d.mean); });
	root.append("defs").append("clipPath").attr("id", "clip" + idPostfix)
		.append("rect").attr("width", uiConfig.width).attr("height", uiConfig.height).attr("x", 1);

	var movingAverageData = null;
	var isVisible = false;

	function redrawLine() {
		root.selectAll(".line" + idPostfix).remove();
		if (isVisible && movingAverageData !== null) {
			root.append("path")
				.datum(movingAverageData)
				.attr("class", "line" + idPostfix)
				.attr("clip-path", "url(#clip" + idPostfix + ")")
				.attr("d", line);
		}
	}

	var it = {};
	it.update = function(update) {
		var getValue = function(it) { return it.y; };
		var getDate = function(it) { return it.x; };
		movingAverageData = movingAverageForTimedValues(update.dataStacked[0], update.dataTimeInterval, getDate, getValue);
		redrawLine();
	};
	it.onXScaleUpdate = function() {
		redrawLine();
	};
	it.setVisible = function(value) {
		isVisible = value;
		redrawLine();
	};
	return it;
}

function movingAverageForTimedValues(data, timeInterval, getDate, getValue, period) {
	if (data.length < 2) return [];

	period = (period === undefined ? Math.round(data.length / 10) : period);
	if (period < 2) return [];

	var firstDate = getDate(data[0]);
	var lastDatePlusOne = timeInterval.offset(getDate(data[data.length - 1]), 1);
	var allDates = timeInterval.range(firstDate, lastDatePlusOne);
	if (allDates.length < period) return [];

	data = valuesForEachDate(data, allDates, getDate, getValue);

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

function valuesForEachDate(data, datesRange, getDate, getValue) {
	var result = {};
	datesRange.forEach(function(date) { result[date] = 0; });
	data.forEach(function(d) { result[getDate(d)] = getValue(d); });
	return result;
}


function newTotalAmountLabel(root, label) {
	var leftFooter = root.append("span");
	leftFooter.append("label").style("color", "#999").html(label);
	var totalAmountLabel = leftFooter.append("label").style("color", "#999").html("");

	var data = null;
	var xScale = null;

	var it = {};
	it.update = function(update) {
		data = update.dataStacked[0];
		updateTotalAmount();
	};
	it.onXScaleUpdate = function(updatedXScale) {
		xScale = updatedXScale;
		updateTotalAmount();
	};
	return it;

	function formatValue(n) {
		var s = d3.format(",")(n);
		return s.length < 3 ? s : d3.format(".3s")(n);
	}
	function totalValueAmountWithin(dateRange, data, getDate, getValue) {
		var withinDomain = function(d) { return getDate(d) >= dateRange[0] && getDate(d) < dateRange[1]; };
		return d3.sum(data.filter(withinDomain), getValue);
	}
	function updateTotalAmount() {
		if (data === null || xScale === null) return;
		var getValue = function(it) { return it.y; };
		var getDate = function(it) { return it.x; };
		var amount = totalValueAmountWithin(xScale.domain(), data, getDate, getValue);
		totalAmountLabel.html(formatValue(amount));
	}
}

function percentileDropDown(root, data) {
	var optionLabels = [];
	for (var i = 100; i >= 95; i -= 0.5) {
		optionLabels.push(i);
	}
	newDropDown(root, "Percentile: ", optionLabels,
		function() { return 0; },
		function(newValue) {
			data.setPercentile(+optionLabels[newValue] / 100);
		}
	);
}


function removeChildrenOf(elementId) {
	var element = document.getElementById(elementId);
	while (element.children.length > 0) {
		var child = element.children.item(0);
		child.parentNode.removeChild(child);
	}
}

// originally from http://stackoverflow.com/questions/5560248/programmatically-lighten-or-darken-a-hex-color-and-blend-colors
function shadeColor(color, percent) {
	if (color.indexOf("rgb") > -1) {
		var hex = color
			.split("(")[1].split(")")[0].split(",")
		    .map(function(x) { return parseInt(x); });
		return doShade(hex[0], hex[1], hex[2], percent);
	} else {
		var f = parseInt(color.slice(1), 16),
			R = f >> 16,
			G = f >> 8 & 0x00FF,
			B = f & 0x0000FF;
		return doShade(R, G, B, percent);
	}

	function doShade(R, G, B, percent) {
		var t = percent < 0 ? 0 : 255;
		var p = percent < 0 ? percent * -1 : percent;
		return "#" + (
			0x1000000 + (Math.round((t - R) * p) + R) *
			0x10000 + (Math.round((t - G) * p) + G) *
			0x100 + (Math.round((t - B) * p) + B)
			).toString(16).slice(1);
	}
}
