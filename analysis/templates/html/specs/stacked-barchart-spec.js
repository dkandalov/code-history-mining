describe("bars", function () {
	it("", function() {
		// TODO ?
	});

	function bars(element, uiConfig, xScale, yScale, bus) {
		var color = d3.scale.category20();
		var dateFormat = d3.time.format("%d/%m/%Y");
		var valueFormat = function(n) {
			var s = d3.format(",")(n);
			return s.length < 6 ? s : d3.format("s")(n);
		};

		bus.on("dataUpdate", function(update) {
			var barWidth = Math.floor((uiConfig.width / xScale.range.length) - 0.5);
			var layer = element.selectAll(".layer")
				.data(update.dataStacked)
				.enter().append("g")
				.attr("class", "layer")
				.style("fill", function(d, i) { return color(i); });

			layer.selectAll("rect")
				.data(function(d) { return d; })
				.enter().append("rect")
				.attr("x", function(d) { return xScale(d.x); })
				.attr("y", function(d) { return yScale(d.y0 + d.y); })
				.attr("width", barWidth)
				.attr("height", function(d) { return yScale(d.y0) - yScale(d.y0 + d.y); })
				.append("title")
				.text(function (d) {
					return "date: " + dateFormat(d.x) + "\n" +
						"value: " + valueFormat(d.y) + "\n" +
						"category: " + d["category"];
				});
		});
	}
});

describe("x axis", function () {
	it("", function() {
		// TODO ?
	});

	function xAxis(x) {
		return d3.svg.axis().scale(x).orient("bottom");
	}
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
});
