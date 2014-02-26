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


function newTooltip(root, uiConfig, tooltipCss) {
	var div = root.append("div")
		.attr("class", tooltipCss)
		.style("position", "absolute")
		.style("opacity", 0);

	var lastUpdate = null;

	var it = {};
	it.update = function(update) {
		lastUpdate = update;
		if (update == null) {
			// use timeout because when moving between two elements, onMouseOver event for next element
			// can come before onMouseOut for old one, what results in tooltip disappearing
			setTimeout(function() {
				if (lastUpdate == null)
					div.transition().delay(0).style("opacity", 0);
			}, 100);
		} else {
			var x = parseInt(update.bar.getAttribute("x"));
			var y = parseInt(update.bar.getAttribute("y"));
			var width = parseInt(update.bar.getAttribute("width"));
			var height = parseInt(update.bar.getAttribute("height"));

			var dateFormat = d3.time.format("%d/%m/%Y");
			var valueFormat = function(n) {
				var s = d3.format(",")(n);
				return s.length < 6 ? s : d3.format("s")(n);
			};

			var left = uiConfig.margin.left + x + width + 5;
			if (left > uiConfig.margin.left + uiConfig.width) left = uiConfig.margin.left + uiConfig.width;
			var top = uiConfig.margin.top + y + (height / 2);
			div.html("Value: " + valueFormat(update.value) + "<br/>" +
					"Date: " + dateFormat(update.date) + "<br/>" +
					"Category: " + update.category)
				.style("left", left + "px")
				.style("top", top + "px");

			var actualHeight = div[0][0].offsetHeight;
			top = uiConfig.margin.top + y + (height / 2 - actualHeight / 2);
			div.style("top",  top + "px");

			div.transition().delay(1500).style("opacity", 0.9);
		}
	};
	return it;
}

// based on https://gist.github.com/ZJONSSON/3918369
function newLegend(root, uiConfig) {
	var it = {};
	it.update = function(items) {
		// always redraw legend so that it would be above graph
		root.select(".legend").remove();
		var legend = root.append("g").attr("class", "legend")
			.attr("transform", "translate(" + (uiConfig.width - 50) + ",20)");

		var box = legend.append("rect").attr("class", "box");
		var itemList = legend.append("g").attr("class", "items");

		itemList.selectAll("text")
			.data(items)
			.call(function(d) { d.enter().append("text") })
			.attr("y", function(d, i) { return i + "em"; })
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

function newGroupByDropDown(root, stackedData, label, groupingNames) {
	return newDropDown(root, label, groupingNames, function(newValue) {
		stackedData.groupBy(newValue);
	});
}

function newGroupIndexDropDown(root, stackedData, label, groupNames) {
	return newDropDown(root, label, groupNames, function(newValue) {
		stackedData.setGroupIndex(newValue);
	});
}

function newDropDown(root, label, optionLabels, onChange) {
	root.append("label").html(label);
	var dropDown = root.append("select");
	for (var i = 0; i < optionLabels.length; i++) {
		dropDown.append("option").attr("value", i).html(optionLabels[i]);
	}

	dropDown.on("change", function() {
		onChange(this.value)
	});

	return {};
}

function newXBrush(root, uiConfig, xScale, height, y) {
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

		var bars = newBars(g, brushUiConfig, brushXScale, brushYScale, {id: "brushBars", shading: false});
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

function newBars(root, uiConfig, xScale, yScale, settings) {
	var color = d3.scale.category20();
	if (settings == null) settings = {id: "bars", shading: true};
	var id = "-" + settings.id;
	var data;

	root.append("defs").append("clipPath").attr("id", "clip" + id)
		.append("rect").attr("width", uiConfig.width).attr("height", uiConfig.height).attr("x", 1);

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
		data = update.data;

		root.selectAll(".layer" + id).remove();

		var layer = root.selectAll(".layer" + id)
			.data(update.dataStacked)
			.enter().append("g")
			.attr("class", "layer" + id)
			.style("fill", function(d, i) { return color(i); });

		// from http://stackoverflow.com/questions/5560248/programmatically-lighten-or-darken-a-hex-color-and-blend-colors
		function shadeColor(color, percent) {
			var f = parseInt(color.slice(1), 16), t = percent < 0 ? 0 : 255, p = percent < 0 ? percent * -1 : percent, R = f >> 16, G = f >> 8 & 0x00FF, B = f & 0x0000FF;
			return "#" + (0x1000000 + (Math.round((t - R) * p) + R) * 0x10000 + (Math.round((t - G) * p) + G) * 0x100 + (Math.round((t - B) * p) + B)).toString(16).slice(1);
		}

		var rect = layer.selectAll("rect")
			.data(function(d) { return d; })
			.enter().append("rect")
			.attr("clip-path", "url(#clip" + id + ")")
			.attr("x", function(d) { return xScale(d.x); })
			.attr("y", function(d) { return yScale(d.y0 + d.y); })
			.attr("width", barWidth())
			.attr("height", function(d) { return yScale(d.y0) - yScale(d.y0 + d.y); });

		rect.on("mouseover", function(d) {
			if (settings.shading) {
				this.style.fill = shadeColor(this.parentNode.style.fill, -0.15);
			}
			notifyHoverListeners({bar: this, value: d.y, date: d.x, category: d["category"]});
		}).on("mouseout", function() {
			if (settings.shading) {
				this.removeAttribute("style");
			}
			notifyHoverListeners(null);
		});

		var categoryUpdate = [];
		update.dataStacked.forEach(function(it, i) {
			categoryUpdate.push({ category: it[0]["category"], color: color(i) });
		});
		notifyCategoryListeners(categoryUpdate);
	};
	it.onXScaleUpdate = function(updatedXScale) {
		xScale = updatedXScale;

		var layer = root.selectAll(".layer" + id).data(data);
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
	function lengthOf(domain, timeInterval) {
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
		timeInterval = update.groupByTimeInterval;
		x.domain([update.minX, timeInterval.offset(update.maxX, 1)]);
		x.amountOfValues = lengthOf(x.domain(),timeInterval);
	};
	var notifyListeners = observable(x);
	x.setDomain = function(extent) {
		x.amountOfValues = lengthOf(extent, timeInterval);
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


function newStackedData(rawCsv) {
	function groupByCategory(data) {
		var dateFormat = d3.time.format("%d/%m/%Y");
		return d3.nest().key(function(d){ return d["category"]; }).entries(data)
			.map(function (entry) {
				return entry.values.map(function (d) {
					return {
						x: dateFormat.parse(d.date),
						y: parseInt(d["value"]),
						category: d["category"]
					};
				});
			});
	}
	var originalData = rawCsv.map(function(it) { return groupByCategory(d3.csv.parse(it)); });
	var timeIntervals = [d3.time.day, d3.time.monday, d3.time.month];

	var data;
	var dataStacked;
	var groupIndex = 0;
	var groupByIndex = 0;
	var minX;
	var maxX;
	var minY = 0;
	var maxY;

	function groupBy(timeInterval, daysData) {
		if (timeInterval == d3.time.day) return daysData;
		return d3.values(d3.nest()
				.key(function(d) { return timeInterval.floor(d.x); })
				.rollup(function(days) {
					var aggregateValue = d3.sum(days, function (d) { return d.y; });
					var date = timeInterval.floor(days[0].x);
					var category = days[0].category;
					return { category: category, x: date, y: aggregateValue };
				})
				.map(daysData));
	}

	function updateWith(newData) {
		data = newData;
		dataStacked = d3.layout.stack()(data);
		minX = d3.min(data, function(d) {
			return d3.min(d, function(dd) {
				return dd.x;
			});
		});
		maxX = d3.max(data, function(d) {
			return d3.max(d, function(dd) {
				return dd.x;
			});
		});
		maxY = d3.max(dataStacked, function(layer) {
			return d3.max(layer, function(d) {
				return d.y0 + d.y;
			});
		});
	}
	updateWith(originalData[groupIndex].map(function(it) {
		return groupBy(timeIntervals[groupByIndex], it);
	}));


	var it = {};
	var notifyListeners = observable(it);
	it.sendUpdate = function() {
		notifyListeners({
			data: data,
			dataStacked: dataStacked,
			groupIndex: groupIndex,
			groupByIndex: groupByIndex,
			groupByTimeInterval: timeIntervals[groupByIndex],
			minX: minX,
			maxX: maxX,
			minY: minY,
			maxY: maxY
		});
	};
	it.setGroupIndex = function(value) {
		groupIndex = value;
		updateWith(originalData[groupIndex].map(function(it) {
			return groupBy(timeIntervals[groupByIndex], it);
		}));
		it.sendUpdate();
	};
	it.groupBy = function(value) {
		groupByIndex = value;
		updateWith(originalData[groupIndex].map(function(it) {
			return groupBy(timeIntervals[groupByIndex], it);
		}));
		it.sendUpdate();
	};

	return it;
}


