describe("bar chart data", function () {
	it("after construction can broadcast update with stacked data", function() {
		var bus = d3.dispatch("dataUpdate");
		var data = stackedData(rawData, bus);
		var received = null;
		bus.on("dataUpdate", function(data) {
			received = data;
		});

		data.sendUpdate();

		expect(received.length).toEqual(3);
		expect(received[0][0]["category"]).toEqual("Mee");
		expect(received[1][0]["category"]).toEqual("Ooo");
		expect(received[2][0]["category"]).toEqual("Ggg");
	});

	it("when asked to filter data by categories it will broadcast update", function() {
		var bus = d3.dispatch("dataUpdate");
		var data = stackedData(rawData, bus);
		var received = null;
		bus.on("dataUpdate", function(data) {
			received = data;
		});

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

	function stackedData(rawCsv, bus) {
		var dateFormat = d3.time.format("%d/%m/%Y");
		var originalData = d3.csv.parse(rawCsv);
		originalData = d3.nest().key(function(d) { return d["category"]; }).entries(originalData)
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
		var data = originalData;

		var f = function() {};
		f.sendUpdate = function() {
			bus.dataUpdate(data);
		};
		f.useCategories = function(categories) {
			data = originalData.map(function(it) {
				return (categories.indexOf(it[0]["category"]) != -1) ? it : null;
			}).filter(function(it) { return it != null; });
			f.sendUpdate();
		};

		return f;
	}

});
