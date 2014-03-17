describe("'nothing to show' label", function () {
	var rootElement, svgRoot, uiConfig;
	beforeEach(function() {
		rootElement = d3.select("body").append("span").attr("id", "tooltip-test");
		svgRoot = rootElement.append("svg");
		uiConfig = { width: 1000, height: 500, margin: {left: 0, top: 0} };
	});
	afterEach(function() {
		rootElement.remove();
	});

	it("becomes visible if there is no data", function() {
		var label = newNothingToShowLabel(rootElement, svgRoot, uiConfig);

		label.update({ maxY: 123 });
		expect(rootElement.select("div")[0][0].getAttribute("style")).toContain("opacity: 0");

		label.update({ maxY: 0 });
		expect(rootElement.select("div")[0][0].getAttribute("style")).toContain("opacity: 0.9");
	});
});

describe("tooltip", function () {
	var rootElement, svgRoot, uiConfig;
	beforeEach(function() {
		rootElement = d3.select("body").append("span").attr("id", "tooltip-test");
		svgRoot = rootElement.append("svg");
		uiConfig = { width: 1000, height: 500, margin: {left: 0, top: 0} };
	});
	afterEach(function() {
		rootElement.remove();
	});

	it("becomes visible on non-null update", function() {
		var tooltip = newTooltip(rootElement, svgRoot, uiConfig, {delay: 0, delayBeforeHide: 0});
		expect(rootElement.select("div")[0][0].getAttribute("style")).toContain("opacity: 0");

		var bar = document.createElement('rect');
		tooltip.update({
			bar: bar,
			value: 123,
			date: date("23/10/2011"),
			category: "java"
		});

		setTimeout(function() {
			expect(rootElement.select("div")[0][0].getAttribute("style")).toContain("opacity: 0.9");
		}, 10);
	});
});

describe("bars", function () {
	var rootElement, uiConfig, data, x, y;
	beforeEach(function() {
		rootElement = d3.select("body").append("span").attr("id", "bars-test");
		uiConfig = { width: 1000, height: 500 };
		x = newXScale(uiConfig);
		y = newYScale(uiConfig);
		data = newMultipleStackedData(rawData);
	});
	afterEach(function() {
		rootElement.remove();
	});

	it("on data update adds svg rects to root element", function() {
		var bars = newBars(rootElement, uiConfig, x, y, "bars");
		data.onUpdate([x.update, y.update, bars.update]);

		data.sendUpdate();

		expect(rootElement.selectAll(".layer-bars")[0].length).toEqual(3);
		expect(rootElement.selectAll(".layer-bars rect")[0].length).toEqual(9);
		rootElement.selectAll(".layer-bars rect")[0].map(function(it) {
			expect(parseInt(it.attributes["width"].value)).toBeGreaterThan(0);
			expect(parseInt(it.attributes["width"].value)).toBeLessThan(uiConfig.height + 1);
			expect(parseInt(it.attributes["height"].value)).toBeGreaterThan(0);
			expect(parseInt(it.attributes["height"].value)).toBeLessThan(uiConfig.height + 1);
		});
	});

	it("on data update replaces svg rects with new ones according to new data", function() {
		function allRectsHeight() {
			var allRects = rootElement.selectAll(".layer-bars rect")[0];
			return _.reduce(allRects, function(acc, it){ return acc + parseInt(it.attributes["height"].value); }, 0);
		}
		var bars = newBars(rootElement, uiConfig, x, y, "bars");
		data.onUpdate([x.update, y.update, bars.update]);

		data.sendUpdate();
		expect(allRectsHeight()).toEqual(995);

		data.setGroupIndex(1);
		expect(allRectsHeight()).toEqual(997);
	});

	it("on data update sends categories and their colors to listeners", function() {
		var bars = newBars(rootElement, uiConfig, x, y, "bars");
		data.onUpdate([x.update, y.update, bars.update]);
		var received = null;
		bars.onUpdate([function(update) {
			received = update;
		}]);

		data.sendUpdate();

		expect(received).toEqual([
			{category: "java", color: '#1f77b4'},
			{category: "xml", color: '#aec7e8'},
			{category: "txt", color: '#ff7f0e'}
		]);
	});
});

describe("x scale", function () {
	it("sends update when its domain is changed", function() {
		var x = newXScale({ width: 100 });
		var updatedX = null;
		x.onUpdate([function(x) {
			updatedX = x;
		}]);
		var data = {minX: date("20/04/2011"), maxX: date("30/04/2011"), dataTimeInterval: d3.time.day};
		x.update(data);

		x.setDomain([date("20/04/2011"), date("25/04/2011")]);

		expect(updatedX.amountOfValues).toEqual(5);
		expect(updatedX.domain()).toEqual([date("20/04/2011"), date("25/04/2011")]);
	});
});

describe("bar chart data", function () {
	it("after construction it can broadcast update with stacked data", function() {
		var data = newStackedData(rawData[0]);
		var received = null;
		data.onUpdate([function(update) {
			received = update;
		}]);

		data.sendUpdate();

		expect(received.dataStacked.length).toEqual(3);
		expect(received.dataStacked[0][0]).toEqual({ category: "java", x: date("18/01/2013"), y: 1, y0: 0 });
		expect(received.dataStacked[1][0]).toEqual({ category: "xml", x: date("18/01/2013"), y: 11, y0: 1 });
		expect(received.dataStacked[2][0]).toEqual({ category: "txt", x: date("18/01/2013"), y: 111, y0: 1 + 11 });
		expect(received.dataStacked[0][1]).toEqual({ category: "java", x: date("19/01/2013"), y: 2, y0: 0 });
		expect(received.dataStacked[1][1]).toEqual({ category: "xml", x: date("19/01/2013"), y: 22, y0: 2 });
		expect(received.dataStacked[2][1]).toEqual({ category: "txt", x: date("19/01/2013"), y: 222, y0: 2 + 22 });
	});

	it("sends update with regrouped data when asked to group by different time interval", function() {
		var data = newStackedData(rawData[0]);
		var received = null;
		data.onUpdate([function(update) {
			received = update;
		}]);

		data.sendUpdate();
		expect(received.groupByIndex).toEqual(0);
		expect(received.dataTimeInterval).toEqual(d3.time.day);
		expect(received.dataStacked[0][0]).toEqual({ category: "java", x: date("18/01/2013"), y: 1, y0: 0 });

		data.groupBy(1);
		expect(received.groupByIndex).toEqual(1);
		expect(received.dataTimeInterval).toEqual(d3.time.monday);
		expect(received.dataStacked[0][0]).toEqual({ category: "java", x: date("14/01/2013"), y: 1 + 2 + 3, y0: 0 });
		expect(received.dataStacked[1][0]).toEqual({ category: "xml", x: date("14/01/2013"), y: 11 + 22 + 33, y0: 6 });
		expect(received.dataStacked[2][0]).toEqual({ category: "txt", x: date("14/01/2013"), y: 111 + 222+ 333, y0: 72 });
	});

	it("sends update with new data when group index changes", function() {
		var data = newMultipleStackedData(rawData);
		var received = null;
		data.onUpdate([function(update) {
			received = update;
		}]);

		data.sendUpdate();
		expect(received.groupIndex).toEqual(0);
		expect(received.dataStacked[0][0]["category"]).toEqual("java");
		expect(received.dataStacked[0][0]["y"]).toEqual(1);

		data.setGroupIndex(1);
		expect(received.groupIndex).toEqual(1);
		expect(received.dataStacked[0][0]["category"]).toEqual("java");
		expect(received.dataStacked[0][0]["y"]).toEqual(11);
	});
});

describe("moving average line", function() {
	var rootElement, uiConfig, data, x, y;
	beforeEach(function() {
		rootElement = d3.select("body").append("span").attr("id", "bars-test");
		uiConfig = { width: 1000, height: 500 };
		x = newXScale(uiConfig);
		y = newYScale(uiConfig);
		data = newMultipleStackedData(rawData);
	});
	afterEach(function() {
		rootElement.remove();
	});

	it("on data update adds svg line to root element", function() {
		var movingAverage = newMovingAverageLine(rootElement, uiConfig, x, y, "movingAverage");
		movingAverage.setVisible(true);
		data.onUpdate([movingAverage.update]);

		data.sendUpdate();

		expect(rootElement.selectAll(".line-movingAverage")[0].length).toEqual(1);
	});

	it("can become invisible", function() {
		var movingAverage = newMovingAverageLine(rootElement, uiConfig, x, y, "movingAverage");
		movingAverage.setVisible(true);
		data.onUpdate([movingAverage.update]);

		data.sendUpdate();

		expect(rootElement.selectAll(".line-movingAverage")[0].length).toEqual(1);
		movingAverage.setVisible(false);
		expect(rootElement.selectAll(".line-movingAverage")[0].length).toEqual(0);
	});
});

describe("moving average for timed values", function () {
	var getDate = function(it) { return it.date; };
	var getValue = function(it) { return it.value; };

	it("for three day interval", function() {
		var movingAverageOfThreeDays = function(data) {
			return movingAverageForTimedValues(data, d3.time.day, getDate, getValue, 3);
		};

		expect(movingAverageOfThreeDays([
			{date: date("01/01/2010"), value: 10},
			{date: date("02/01/2010"), value: 10}
		])).toEqual([]);

		expect(movingAverageOfThreeDays([
			{date: date("01/01/2010"), value: 11},
			{date: date("03/01/2010"), value: 13},
			{date: date("04/01/2010"), value: 5}
		])).toEqual([
				{date: date("03/01/2010"), mean: (11 + 13) / 3},
				{date: date("04/01/2010"), mean: (13 + 5) / 3}
			]);
	});

	it("for three month interval", function() {
		var movingAverageOfThreeWeeks = function(data) {
			return movingAverageForTimedValues(data, d3.time.month, getDate, getValue, 3);
		};

		expect(movingAverageOfThreeWeeks([
			{date: date("01/01/2010"), value: 10},
			{date: date("01/02/2010"), value: 10}
		])).toEqual([]);

		expect(movingAverageOfThreeWeeks([
			{date: date("01/01/2010"), value: 10},
			{date: date("01/02/2010"), value: 10},
			{date: date("01/03/2010"), value: 10},
			{date: date("01/04/2010"), value: 40}
		])).toEqual([
				{date: date("01/03/2010"), mean: 10},
				{date: date("01/04/2010"), mean: 20}
			]);
	});
});

function date(s) {
	return d3.time.format("%d/%m/%Y").parse(s);
}

var rawData = ["\
date,category,value\n\
18/01/2013,java,1\n\
19/01/2013,java,2\n\
20/01/2013,java,3\n\
18/01/2013,xml,11\n\
19/01/2013,xml,22\n\
20/01/2013,xml,33\n\
18/01/2013,txt,111\n\
19/01/2013,txt,222\n\
20/01/2013,txt,333\n\
",
"date,category,value\n\
18/01/2013,java,11\n\
19/01/2013,java,22\n\
20/01/2013,java,33\n\
18/01/2013,xml,111\n\
19/01/2013,xml,222\n\
20/01/2013,xml,333\n\
18/01/2013,txt,1111\n\
19/01/2013,txt,2222\n\
20/01/2013,txt,3333\n\
"];
