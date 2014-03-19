package analysis.templates
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class TemplateTest {
	@Test void "fills in project name in js literal"() {
		def template = new Template("var name = /*project_name_placeholder*/\"some project\"/*project_name_placeholder*/;")
		assert template.fillProjectName("myProject").text == "var name = \"myProject\";"
	}

	@Test void "fills in project name in html tag"() {
		def template = new Template("<title>{{project-name}}</title>")
		assert template.fillProjectName("myProject").text == "<title>myProject</title>"
	}

	@Test void "fills in data placeholder"() {
		def template = new Template("var data = /*data_placeholder*/[1,2,3]/*data_placeholder*/;")
		assert template.fillData("[5,6,7]").text ==  "var data = [5,6,7];"
	}

	@Test void "inlines javascript libraries"() {
		def template = new Template("""
			<body>
				<script src="lib1.js"></script>
				<script src="lib2.js"></script>
			</body>
		""")

		def fileReader = {
			if (it == "lib1.js") "// lib1 code"
			else if (it == "lib2.js") "// lib2 code"
		}

		assertThat(template.inlineImports(fileReader).text, equalTo("""
			<body>
				<script>// lib1 code</script>
				<script>// lib2 code</script>
			</body>
		"""))
	}

	@Test void "inlines imported css files"() {
		def template = new Template("""
			<style>css0 {}</style>
			<head>
				<stuff/>
				<link rel="stylesheet" type="text/css" media="screen" href="stylesheet1.css">
				<link rel="stylesheet" type="text/css" href="stylesheet2.css">
			</head>
		""")

		def fileReader = {
			if (it == "stylesheet1.css") "css1 {}"
			else if (it == "stylesheet2.css") "css2 {}"
		}

		assertThat(template.inlineImports(fileReader).text, equalTo("""
			<style>css0 {}</style>
			<head>
				<stuff/><style>css1 {}</style><style>css2 {}</style></head>
		"""))
	}

	@Test void "appends style tag at insert point"() {
		def insertPoint = "<!--style-insert-point-->"
		def template = new Template("""
			<style>
			</style>
			${insertPoint}
		""")

		assertThat(template.addBefore(insertPoint, "<style>css{}</style>").text, equalTo("""
			<style>
			</style>
			<style>css{}</style>${insertPoint}
		""".toString()))
	}

	@Test void "extracts last style tag"() {
		def template = new Template("""
			<meta charset="utf-8">
			<style>css1 {}</style>
			<style>css2 {}</style>
		  <body></body>
		""")
		assert template.lastTag("style") == "<style>css2 {}</style>"
	}

	@Test void "extracts content of title tag"() {
		def template = new Template("""
			<meta charset="utf-8">
			<title>A title</title>
		  <body></body>
		""")
		assert template.contentOfTag("title") == "A title"
	}

	@Test void "gets id from span tag"() {
		def template = new Template("""
			<body>
			<span id="change-size-chart"></span>
		  </body>
		""")
		assert template.mainTagId == "change-size-chart"
	}
}
