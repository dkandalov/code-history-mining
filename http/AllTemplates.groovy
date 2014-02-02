package http

class AllTemplates {
	static changeSizeChartTemplate = new Template(HttpUtil.readFile("changes-size-chart.html")).inlineImports(HttpUtil.&readFile)

}
