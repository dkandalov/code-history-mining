function observable(target) {
	var listeners = [];
	target.onUpdate = function(newListeners) {
		listeners = newListeners;
	};
	return function(update) {
		for (var i = 0; i < listeners.length; i++) {
			listeners[i](update);
		}
	};
}

function newControlsPanel(root, uiConfig) {
	return root.append("span").style({display: "block", width: uiConfig.width + "px"})
			.append("span").style({float: "right"});
}

function groupIndexDropDown(root, stackedData, label, groupNames) {
	root.append("label").html(label);
	var dropDown = root.append("select");
	for (var i = 0; i < groupNames.length; i++) {
		dropDown.append("option").attr("value", i).html(groupNames[i]);
	}

	dropDown.on("change", function() {
		stackedData.setGroupIndex(this.value)
	});

	return {};
}

function newXBrush(root, uiConfig, xScale, height, y) {
	height = height == null ? 50 : height;
	var shift = -10; // no deep meaning just a tweak
	y = y == null ? (uiConfig.height + uiConfig.margin.top + shift) : y;

	var brushXScale = d3.time.scale().nice().rangeRound([0, uiConfig.width]);
	var brush = d3.svg.brush().x(brushXScale);

	brush.update = function() {
		brushXScale.domain(xScale.domain());
		var extent = brush.empty() ? brushXScale.domain() : brush.extent();
		xScale.setDomain(extent);
	};
	brush.on("brush", function() {
		var extent = brush.empty() ? brushXScale.domain() : brush.extent();
		xScale.setDomain(extent);
	});
	var g = root.append("g")
		.attr("transform", "translate(0" + "," + y + ")");
	g.append("g")
		.attr("class", "x brush")
		.call(brush)
		.selectAll("rect")
		.attr("height", height);

	return brush;
}

function newBars(root, uiConfig, xScale, yScale) {
	var color = d3.scale.category20();
	var dateFormat = d3.time.format("%d/%m/%Y");
	var valueFormat = function(n) {
		var s = d3.format(",")(n);
		return s.length < 6 ? s : d3.format("s")(n);
	};
	var data;

	root.append("defs").append("clipPath").attr("id", "barsClip")
		.append("rect").attr("width", uiConfig.width).attr("height", uiConfig.height);

	function nonZero(length) {
		return (length > 0 ? length : 0.0000001);
	}
	function barWidth() {
		var barWidth = uiConfig.width / nonZero(xScale.amountOfValues);
		barWidth = Math.floor(barWidth) - 2;
		barWidth = barWidth < 1 ? 1 : barWidth;
		return barWidth;
	}

	var it = {};
	it.update = function(update) {
		data = update.data;

		root.selectAll(".layer").remove();

		var layer = root.selectAll(".layer")
			.data(update.dataStacked)
			.enter().append("g")
			.attr("class", "layer")
			.style("fill", function(d, i) { return color(i); });

		layer.selectAll("rect")
			.data(function(d) { return d; })
			.enter().append("rect")
			.attr("clip-path", "url(#barsClip)")
			.attr("x", function(d) { return xScale(d.x); })
			.attr("y", function(d) { return yScale(d.y0 + d.y); })
			.attr("width", barWidth())
			.attr("height", function(d) { return yScale(d.y0) - yScale(d.y0 + d.y); })
			.append("title")
			.text(function (d) {
				return "date: " + dateFormat(d.x) + "\n" +
					"value: " + valueFormat(d.y) + "\n" +
					"category: " + d["category"];
			});
	};
	it.onXScaleUpdate = function(updatedXScale) {
		xScale = updatedXScale;

		var layer = root.selectAll(".layer").data(data);
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


function xScale(uiConfig) {
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

	var timeInterval = d3.time["day"];
	var x = d3.time.scale().nice().rangeRound([0, uiConfig.width]);
	x.update = function(update) {
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

function yScale(uiConfig) {
	var y = d3.scale.linear().range([uiConfig.height, 0]);
	y.update = function(update) {
		y.domain([update.minY, update.maxY]);
	};
	return y;
}


function stackedData(rawCsv) {
	function groupByCategory(data) {
		var dateFormat = d3.time.format("%d/%m/%Y");
		return d3.nest().key(function(d){ return d["category"]; }).entries(data)
			.map(function (entry) {
				return entry.values.map(function (d) {
					return {
						x: dateFormat.parse(d.date),
						y: parseInt(d["value"]),
						category: d["category"],
						y0: 0
					};
				});
			});
	}
	var originalData = rawCsv.map(function(it) { return groupByCategory(d3.csv.parse(it)); });

	var data;
	var dataStacked;
	var groupIndex = 0;
	var minX;
	var maxX;
	var minY = 0;
	var maxY;

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
	updateWith(originalData[groupIndex]);


	var it = {};
	var notifyListeners = observable(it);
	it.sendUpdate = function() {
		notifyListeners({
			data: data,
			dataStacked: dataStacked,
			groupIndex: groupIndex,
			minX: minX,
			maxX: maxX,
			minY: minY,
			maxY: maxY
		});
	};
	it.setGroupIndex = function(value) {
		groupIndex = value;
		updateWith(originalData[groupIndex]);
		it.sendUpdate();
	}

	return it;
}

