describe("quick find", function () {
	it("finds if points are connected", function() {
		var quickFind = new QuickFind(3);
		expect(quickFind.areConnected(1, 2)).toBe(false);
		expect(quickFind.areConnected(1, 3)).toBe(false);

		quickFind.connect(1, 2);
		expect(quickFind.areConnected(1, 2)).toBe(true);
		expect(quickFind.areConnected(1, 3)).toBe(false);

		quickFind.connect(2, 3);
		expect(quickFind.areConnected(1, 2)).toBe(true);
		expect(quickFind.areConnected(1, 3)).toBe(true);
	});
});
