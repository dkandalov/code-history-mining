describe("bars", function () {
	it("on data update add svg rects to root element", function() {
		var rootElement = d3.select("body").append("span").attr("id", "bars-test");
		var uiConfig = { width: 1000, height: 500 };
		var x = xScale(uiConfig);
		var y = yScale(uiConfig)
		var data = stackedData(rawData);
		var bars = newBars(rootElement, uiConfig, x, y);
		data.onUpdate([x.update, y.update, bars.update]);

		data.sendUpdate();

		expect(rootElement.selectAll(".layer")[0].length).toEqual(3);
		expect(rootElement.selectAll(".layer rect")[0].length).toEqual(9);
		rootElement.selectAll(".layer rect")[0].map(function(it) {
			expect(parseInt(it.attributes["width"].value)).toEqual(331);
			expect(parseInt(it.attributes["height"].value)).toBeGreaterThan(0);
			expect(parseInt(it.attributes["height"].value)).toBeLessThan(500);
		});
	});
});

describe("x scale", function () {
	it("sends update when its domain is changed", function() {
		var x = xScale({ width: 100 });
		var updatedX = null;
		x.onUpdate([function(x) {
			updatedX = x;
		}]);
		var data = {minX: date("20/04/2011"), maxX: date("30/04/2011")};
		x.update(data);

		x.setDomain([date("20/04/2011"), date("25/04/2011")]);

		expect(updatedX.amountOfValues).toEqual(5);
		expect(updatedX.domain()).toEqual([date("20/04/2011"), date("25/04/2011")]);
	});

	function date(s) {
		return d3.time.format("%d/%m/%Y").parse(s);
	}
});

describe("bar chart data", function () {
	it("after construction can broadcast update with stacked data", function() {
		var data = stackedData(rawData);
		var received = null;
		data.onUpdate([function(update) {
			received = update.data;
		}]);

		data.sendUpdate();

		expect(received.length).toEqual(3);
		expect(received[0][0]["category"]).toEqual("Mee");
		expect(received[1][0]["category"]).toEqual("Ooo");
		expect(received[2][0]["category"]).toEqual("Ggg");
	});

	it("when asked to filter data by categories it will broadcast update with filtered data", function() {
		var data = stackedData(rawData);
		var received = null;
		data.onUpdate([function(update) {
			received = update.data;
		}]);

		data.useCategories(["Mee", "Ggg"]);

		expect(received.length).toEqual(2);
		expect(received[0][0]["category"]).toEqual("Mee");
		expect(received[1][0]["category"]).toEqual("Ggg");
		expect(received[0][0]["y"]).toEqual(1);
		expect(received[1][0]["y"]).toEqual(111);
	});
});

var rawData = "\
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
";
