function observable(target, eventName) {
	eventName = (eventName == null ? "onUpdate" : eventName);
	var listeners = [];
	target[eventName] = function(newListeners) {
		if (_.isArray(newListeners)) listeners = newListeners;
		else listeners = [newListeners];
	};
	return function(update) {
		for (var i = 0; i < listeners.length; i++) {
			listeners[i](update);
		}
	};
}

function newHeader(root, uiConfig, name) {
	var headerSpan = root.append("span").style({display: "block", width: uiConfig.width + "px"});
	headerSpan.append("h3").text(name).style({"text-align": "center"});
}


function newNothingToShowLabel(root, svgRoot, uiConfig) {
	var label = root.append("div")
		.html("Unfortunately, there is nothing to show.")
		.style("position", "absolute")
		.style("opacity", 0);
	var it = {};
	it.update = function(update) {
		if (update.maxY > 0) {
			label.style("opacity", 0);
		} else {
			var svgPos = svgRoot[0][0];
			label.style("opacity", 0.9)
				.style("top", function(){
					return svgPos.offsetTop + uiConfig.margin.top + (uiConfig.height / 2) - (this.offsetHeight / 2) + "px";
				})
				.style("left", function(){
					return svgPos.offsetLeft + uiConfig.margin.left + (uiConfig.width / 2) - (this.offsetWidth / 2) + "px";
				});
		}
	};
	return it;
}

function newShading() {
	// from http://stackoverflow.com/questions/5560248/programmatically-lighten-or-darken-a-hex-color-and-blend-colors
	function shadeColor(color, percent) {
		var f = parseInt(color.slice(1), 16), t = percent < 0 ? 0 : 255, p = percent < 0 ? percent * -1 : percent, R = f >> 16, G = f >> 8 & 0x00FF, B = f & 0x0000FF;
		return "#" + (0x1000000 + (Math.round((t - R) * p) + R) * 0x10000 + (Math.round((t - G) * p) + G) * 0x100 + (Math.round((t - B) * p) + B)).toString(16).slice(1);
	}

	var it = {};
	it.update = function(update) {
		if (update.event == "mouseout") {
			update.bar.removeAttribute("style");
		} else {
			update.bar.style.fill = shadeColor(update.bar.parentNode.style.fill, -0.15);
		}
	};
	return it;
}

function newTooltip(root, svgRoot, uiConfig, settings) {
	if (settings == null) settings = {};
	if (settings.showCategory == null) settings.showCategory = true;
	if (settings.css == null) settings.css = "";
	if (settings.delay == null) settings.delay = 1500;
	if (settings.delayBeforeHide == null) settings.delayBeforeHide = 100;

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
		if (update.event == "mouseout") {
			// use timeout because when moving between two elements, onMouseOver event for next element
			// can come before onMouseOut for old one, what results in tooltip disappearing
			setTimeout(function() {
				if (lastUpdate.event == "mouseout")
					div.transition().delay(0).style("opacity", 0);
			}, settings.delayBeforeHide);
		} else {
			var x = parseInt(update.bar.getAttribute("x"));
			var y = parseInt(update.bar.getAttribute("y"));
			var width = parseInt(update.bar.getAttribute("width"));
			var height = parseInt(update.bar.getAttribute("height"));

			var svgPos = svgRoot[0][0];
			var left = svgPos.offsetLeft + uiConfig.margin.left + x + width + 5;
			if (left > svgPos.offsetLeft + uiConfig.margin.left + uiConfig.width) {
				left = svgPos.offsetLeft + uiConfig.margin.left + uiConfig.width;
			}
			var top = svgPos.offsetTop + uiConfig.margin.top + y + (height / 2);

			var text = ["Value: " + valueFormat(update.value), "Date: " + dateFormat(update.date)];
			if (settings.showCategory) text.push("Category: " + update.category);
			div.html(text.join("</br>"))
				.style("left", left + "px")
				.style("top", top + "px");

			// can determine actual height only after adding text
			var actualHeight = div[0][0].offsetHeight;
			top = svgPos.offsetTop + uiConfig.margin.top + y + (height / 2 - actualHeight / 2);
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
			.attr("cy", function(d, i) { return i - 0.4 + "em"; })
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


function newControlsPanel(root, uiConfig) {
	var it = root.append("span").style({display: "block", width: uiConfig.width + "px"})
				 .append("span").style({float: "right"});
	it.addSpace = function() {
		it.append("span").style({width: "20px", display: "inline-block"});
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

function newDropDown(root, label, optionLabels, getSelectedIndex, onChange) {
	root.append("label").html(label);
	var dropDown = root.append("select");
	for (var i = 0; i < optionLabels.length; i++) {
		dropDown.append("option").attr("value", i).html(optionLabels[i]);
	}

	dropDown.on("change", function() {
		onChange(this.value)
	});

	var it = {};
	it.update = function(update) {
		dropDown[0][0].selectedIndex = getSelectedIndex(update);
	};
	return it;
}


function newXBrush(root, uiConfig, xScale, height, y, color) {
	height = height == null ? 50 : height;
	y = y == null ? (uiConfig.height + uiConfig.margin.top) : y;

	var brushUiConfig = _.extend({}, uiConfig, {height: height});
	var brushXScale = newXScale(uiConfig);
	var brushYScale = newYScale(brushUiConfig);
	var brush = d3.svg.brush().x(brushXScale);

	var g = root.append("g").attr("transform", "translate(0" + "," + y + ")");

	function updateUI(update) {
		g.selectAll("g").remove();
		g.selectAll("defs").remove();

		var bars = newBars(g, brushUiConfig, brushXScale, brushYScale, "brushBars", color);
		if (update != null) bars.update(update);

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
}

function newBars(root, uiConfig, xScale, yScale, postfixId, color) {
	postfixId = (postfixId == null ? "-bars" : "-" + postfixId);
	color = (color == null ? d3.scale.category20() : color);
	var dataStacked;

	root.append("defs").append("clipPath").attr("id", "clip" + postfixId)
		.append("rect").attr("width", uiConfig.width).attr("height", uiConfig.height).attr("x", 1);

	function getCategory(d) {
		return d["category"];
	}
	function nonZero(length) {
		return (length > 0 ? length : 0.0000001);
	}
	function barWidth() {
		var barWidth = uiConfig.width / nonZero(xScale.amountOfValues);
		barWidth = Math.floor(barWidth);
		if (barWidth > 20) barWidth = barWidth - 1;
		if (barWidth < 1) barWidth = 1;
		return barWidth;
	}

	var it = {};
	var notifyCategoryListeners = observable(it);
	var notifyHoverListeners = observable(it, "onHover");
	it.update = function(update) {
		dataStacked = update.dataStacked;

		root.selectAll(".layer" + postfixId).remove();

		var layer = root.selectAll(".layer" + postfixId)
			.data(update.dataStacked)
			.enter().append("g")
			.attr("class", "layer" + postfixId)
			.style("fill", function(d, i) { return color(i); });

		var rect = layer.selectAll("rect")
			.data(function(d) { return d; })
			.enter().append("rect")
			.attr("clip-path", "url(#clip" + postfixId + ")")
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

		var layer = root.selectAll(".layer" + postfixId).data(dataStacked);
		layer.selectAll("rect")
			.data(function(d) { return d; })
			.attr("x", function(d) { return xScale(d.x); })
			.attr("width", barWidth());
	};

	return it;
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
}

function newYScale(uiConfig) {
	var y = d3.scale.linear().range([uiConfig.height, 0]);
	y.update = function(update) {
		y.domain([update.minY, update.maxY]);
	};
	return y;
}

function autoGroup(data) {
	function bestTimeIntervalForGrouping(minTime, maxTime) {
		if (new Date(maxTime - minTime) < d3.time.month.offset(new Date(0), 1)) {
			return d3.time.day;
		} else if (new Date(maxTime - minTime) < d3.time.month.offset(new Date(0), 24)) {
			return d3.time.monday;
		} else {
			return d3.time.month;
		}
	}

	var ranOnce = false;
	var it = {};
	it.update = function(update) {
		if (ranOnce) return;
		ranOnce = true;

		var timeInterval = bestTimeIntervalForGrouping(update.minX, update.maxX);
		data.groupBy(timeInterval);
	};
	return it;
}


function stackedData(rawCsv) {
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
						y: parseInt(d["value"]),
						category: getCategory(d)
					};
				});
			});
	}
	var dataStacked = d3.layout.stack()(groupByCategory(d3.csv.parse(rawCsv)));

	var it = {};
	var notifyListeners = observable(it);
	it.sendUpdate = function() {
		notifyListeners({
			dataStacked: dataStacked
		});
	};
	return it;
}

function withMinMax(data) {
	var dataUpdate;
	var it = _.extend({}, data);
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
		notifyListeners(_.extend({}, dataUpdate, {
			minX: minX,
			maxX: maxX,
			minY: minY,
			maxY: maxY
		}));
	};
	return it;
}

function groupedByTime(data) {
	function getCategory(d) {
		return d["category"];
	}
	function groupBy(timeInterval, daysData) {
		if (timeInterval == d3.time.day) return daysData;
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

	var timeIntervals = [d3.time.day, d3.time.monday, d3.time.month];
	var groupByIndex = 0;
	var groupedData;
	var dataUpdate;

	var it = _.extend({}, data);
	var notifyListeners = observable(it);
	data.onUpdate(function(update) {
		dataUpdate = update;
	});
	it.sendUpdate = function() {
		data.sendUpdate();
		regroupStackedData();
		notifyListeners(_.extend({}, dataUpdate, {
			dataStacked: groupedData,
			groupByIndex: groupByIndex,
			dataTimeInterval: timeIntervals[groupByIndex]
		}));
	};
	it.groupBy = function(value) {
		if (!_.isNumber(value)) {
			var i = timeIntervals.indexOf(value);
			if (i == -1) return;
			value = i;
		}
		if (value == groupByIndex) return;

		groupByIndex = value;
		it.sendUpdate();
	};
	return it;
}

function newStackedData(rawCsv) {
	return withMinMax(groupedByTime(stackedData(rawCsv)));
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
	it.groupBy = function(value) {
		for (var i = 0; i < stackedData.length; i++) {
			if (i != groupIndex) stackedData[i].groupBy(value); // skip selected groupIndex to make it send update last
		}
		stackedData[groupIndex].groupBy(value);
		it.sendUpdate();
	};
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
		if (i != -1) it.setGroupIndex(i);
	};
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
	var checkbox = root.append("input").attr("type", "checkbox").on("click", function() {
		movingAverageLine.setVisible(this.checked);
	});
}

function newMovingAverageLine(root, uiConfig, xScale, yScale, postfixId) {
	postfixId = (postfixId == null ? "-movingAvg" : "-" + postfixId);
	var line = d3.svg.line()
		.x(function(d) { return xScale(d.date); })
		.y(function(d) { return yScale(d.mean); });
	root.append("defs").append("clipPath").attr("id", "clip" + postfixId)
		.append("rect").attr("width", uiConfig.width).attr("height", uiConfig.height).attr("x", 1);

	var movingAverageData;
	var isVisible = false;

	function redrawLine() {
		root.selectAll(".line" + postfixId).remove();
		if (isVisible && movingAverageData != null) {
			root.append("path")
				.datum(movingAverageData)
				.attr("class", "line" + postfixId)
				.attr("clip-path", "url(#clip" + postfixId + ")")
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

	period = (period == null ? Math.round(data.length / 10) : period);
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


function newTotalAmountLabel(root, svgRoot, uiConfig, label) {
	var someShift = 30;
	var leftFooter = root.append("span")
		.style("position", "absolute")
		.style("left", function(){ return svgRoot[0][0].offsetLeft + someShift + uiConfig.margin.left + "px"; });
	leftFooter.append("label").style("color", "#999").html(label);

	var totalAmountLabel = leftFooter
		.append("span").style("color", "#999")
		.append("label").html("");

	var data;
	var xScale;

	function formatValue(n) {
		var s = d3.format(",")(n);
		return s.length < 3 ? s : d3.format(".3s")(n);
	}
	function totalValueAmountWithin(dateRange, data, getDate, getValue) {
		var withinDomain = function(d) { return getDate(d) >= dateRange[0] && getDate(d) < dateRange[1]; };
		return d3.sum(data.filter(withinDomain), getValue);
	}
	function updateTotalAmount() {
		if (data == null || xScale == null) return;
		var getValue = function(it) { return it.y; };
		var getDate = function(it) { return it.x; };
		var amount = totalValueAmountWithin(xScale.domain(), data, getDate, getValue);
		totalAmountLabel.html(formatValue(amount));
	}

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
}
