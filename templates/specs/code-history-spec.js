describe("change size chart", function () {
	var date = function(s) { return d3.time.format("%d/%m/%Y").parse(s); };

	it("should calculate rolling average for three day interval", function() {
		var rollingAverageOfThreeDays = function(data) { return rollingAverage(data, d3.time.day, 3); };

		expect(rollingAverageOfThreeDays([
			{date: date("01/01/2010"), changeSize: 10},
			{date: date("02/01/2010"), changeSize: 10}
		])).toEqual([]);

		expect(rollingAverageOfThreeDays([
			{date: date("01/01/2010"), changeSize: 11},
			{date: date("03/01/2010"), changeSize: 13},
			{date: date("04/01/2010"), changeSize: 5}
		])).toEqual([
			{date: date("03/01/2010"), mean: (11 + 13) / 3},
			{date: date("04/01/2010"), mean: (13 + 5) / 3}
		]);
	});

	it("should calculate rolling average for three month interval", function() {
		var rollingAverageOfThreeWeeks = function(data) { return rollingAverage(data, d3.time.month, 3); };

		expect(rollingAverageOfThreeWeeks([
			{date: date("01/01/2010"), changeSize: 10},
			{date: date("01/02/2010"), changeSize: 10}
		])).toEqual([]);

		expect(rollingAverageOfThreeWeeks([
			{date: date("01/01/2010"), changeSize: 10},
			{date: date("01/02/2010"), changeSize: 10},
			{date: date("01/03/2010"), changeSize: 10},
			{date: date("01/04/2010"), changeSize: 40}
		])).toEqual([
			{date: date("01/03/2010"), mean: 10},
			{date: date("01/04/2010"), mean: 20}
		]);
	});
});