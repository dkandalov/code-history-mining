describe("change size chart", function () {
	var date = function(s) { return d3.time.format("%d/%m/%Y").parse(s); };
	var getChangeSize = function(it) { return it.changeSize; };

	it("should calculate moving average for three day interval", function() {
		var movingAverageOfThreeDays = function(data) { return movingAverage(data, d3.time.day, getChangeSize, 3); };

		expect(movingAverageOfThreeDays([
			{date: date("01/01/2010"), changeSize: 10},
			{date: date("02/01/2010"), changeSize: 10}
		])).toEqual([]);

		expect(movingAverageOfThreeDays([
			{date: date("01/01/2010"), changeSize: 11},
			{date: date("03/01/2010"), changeSize: 13},
			{date: date("04/01/2010"), changeSize: 5}
		])).toEqual([
			{date: date("03/01/2010"), mean: (11 + 13) / 3},
			{date: date("04/01/2010"), mean: (13 + 5) / 3}
		]);
	});

	it("should calculate moving average for three month interval", function() {
		var movingAverageOfThreeWeeks = function(data) { return movingAverage(data, d3.time.month, getChangeSize, 3); };

		expect(movingAverageOfThreeWeeks([
			{date: date("01/01/2010"), changeSize: 10},
			{date: date("01/02/2010"), changeSize: 10}
		])).toEqual([]);

		expect(movingAverageOfThreeWeeks([
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