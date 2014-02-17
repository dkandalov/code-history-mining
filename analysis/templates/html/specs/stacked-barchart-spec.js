describe("bars", function () {
	var rootElement, uiConfig, data, x, y;
	beforeEach(function() {
		rootElement = d3.select("body").append("span").attr("id", "bars-test");
		uiConfig = { width: 1000, height: 500 };
		x = newXScale(uiConfig);
		y = newYScale(uiConfig);
		data = newStackedData(rawData);
	});

	it("on data update add svg rects to root element", function() {
		var bars = newBars(rootElement, uiConfig, x, y, "bars");
		data.onUpdate([x.update, y.update, bars.update]);

		data.sendUpdate();

		expect(rootElement.selectAll(".layer-bars")[0].length).toEqual(3);
		expect(rootElement.selectAll(".layer-bars rect")[0].length).toEqual(9);
		rootElement.selectAll(".layer-bars rect")[0].map(function(it) {
			expect(parseInt(it.attributes["width"].value)).toBeGreaterThan(0);
			expect(parseInt(it.attributes["width"].value)).toBeLessThan(500);
			expect(parseInt(it.attributes["height"].value)).toBeGreaterThan(0);
			expect(parseInt(it.attributes["height"].value)).toBeLessThan(500);
		});
	});
});

describe("x scale", function () {
	it("sends update when its domain is changed", function() {
		var x = newXScale({ width: 100 });
		var updatedX = null;
		x.onUpdate([function(x) {
			updatedX = x;
		}]);
		var data = {minX: date("20/04/2011"), maxX: date("30/04/2011"), groupByTimeInterval: d3.time.day};
		x.update(data);

		x.setDomain([date("20/04/2011"), date("25/04/2011")]);

		expect(updatedX.amountOfValues).toEqual(5);
		expect(updatedX.domain()).toEqual([date("20/04/2011"), date("25/04/2011")]);
	});
});

describe("bar chart data", function () {
	it("after construction it can broadcast update with stacked data", function() {
		var data = newStackedData(rawData);
		var received = null;
		data.onUpdate([function(update) {
			received = update;
		}]);

		data.sendUpdate();

		expect(received.data.length).toEqual(3);
		expect(received.data[0][0]).toEqual({ category: "Mee", x: date("18/01/2013"), y: 1, y0: 0 });
		expect(received.data[1][0]).toEqual({ category: "Ooo", x: date("18/01/2013"), y: 11, y0: 1 });
		expect(received.data[2][0]).toEqual({ category: "Ggg", x: date("18/01/2013"), y: 111, y0: 1 + 11 });
		expect(received.data[0][1]).toEqual({ category: "Mee", x: date("19/01/2013"), y: 2, y0: 0 });
		expect(received.data[1][1]).toEqual({ category: "Ooo", x: date("19/01/2013"), y: 22, y0: 2 });
		expect(received.data[2][1]).toEqual({ category: "Ggg", x: date("19/01/2013"), y: 222, y0: 2 + 22 });
		expect(received.dataStacked.length).toEqual(3);
	});

	it("sends update with new data when group index changes", function() {
		var data = newStackedData(rawData);
		var received = null;
		data.onUpdate([function(update) {
			received = update;
		}]);

		data.sendUpdate();
		expect(received.groupIndex).toEqual(0);
		expect(received.data[0][0]["category"]).toEqual("Mee");
		expect(received.data[0][0]["y"]).toEqual(1);

		data.setGroupIndex(1);
		expect(received.groupIndex).toEqual(1);
		expect(received.data[0][0]["category"]).toEqual("Mee");
		expect(received.data[0][0]["y"]).toEqual(10);
	});

	it("sends update with regrouped data when asked to group by different time interval", function() {
		var data = newStackedData(rawData);
		var received = null;
		data.onUpdate([function(update) {
			received = update;
		}]);

		data.sendUpdate();
		expect(received.groupByIndex).toEqual(0);
		expect(received.groupByTimeInterval).toEqual(d3.time.day);
		expect(received.data[0][0]).toEqual({ category: "Mee", x: date("18/01/2013"), y: 1, y0: 0 });

		data.groupBy(1);
		expect(received.groupByIndex).toEqual(1);
		expect(received.groupByTimeInterval).toEqual(d3.time.monday);
		expect(received.data[0][0]).toEqual({ category: "Mee", x: date("14/01/2013"), y: 1 + 2 + 3, y0: 0 });
		expect(received.data[1][0]).toEqual({ category: "Ooo", x: date("14/01/2013"), y: 11 + 22 + 33, y0: 6 });
		expect(received.data[2][0]).toEqual({ category: "Ggg", x: date("14/01/2013"), y: 111 + 222+ 333, y0: 72 });
	});
});

function date(s) {
	return d3.time.format("%d/%m/%Y").parse(s);
}

var rawData = ["\
date,category,value\n\
18/01/2013,Mee,1\n\
19/01/2013,Mee,2\n\
20/01/2013,Mee,3\n\
18/01/2013,Ooo,11\n\
19/01/2013,Ooo,22\n\
20/01/2013,Ooo,33\n\
18/01/2013,Ggg,111\n\
19/01/2013,Ggg,222\n\
20/01/2013,Ggg,333\n\
",
"date,category,value\n\
18/01/2013,Mee,10\n\
19/01/2013,Mee,20\n\
20/01/2013,Mee,30\n\
18/01/2013,Ooo,110\n\
19/01/2013,Ooo,220\n\
20/01/2013,Ooo,330\n\
18/01/2013,Ggg,1110\n\
19/01/2013,Ggg,2220\n\
20/01/2013,Ggg,3330\n\
"];
