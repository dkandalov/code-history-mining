package http
import org.junit.Test

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class TemplateTest {
	@Test void "fill in project name in template"() {
		def template = new Template("var name = /*project_name_placeholder*/\"some project\"/*project_name_placeholder*/;")
		assert template.fillProjectName("myProject").text == "var name = \"myProject\";"
	}

	@Test void "fill in data in a template"() {
		def template = new Template("var data = /*data_placeholder*/[1,2,3]/*data_placeholder*/;")
		assert template.fillData("[5,6,7]").text ==  "var data = [5,6,7];"
	}

	@Test void "inline javascript libraries in template"() {
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

	@Test void "inline css in template"() {
		def template = new Template("""
			<head>
				<stuff/>
				<link rel="stylesheet" type="text/css" media="screen" href="stylesheet1.css">
				<link rel="stylesheet" type="text/css" media="screen" href="stylesheet2.css">
			</head>
		""")

		def fileReader = {
			if (it == "stylesheet1.css") "css1 {}"
			else if (it == "stylesheet2.css") "css2 {}"
		}

		assertThat(template.inlineImports(fileReader).text, equalTo("""
			<head>
				<stuff/><style>css1 {}</style><style>css2 {}</style></head>
		"""))
	}

	@Test void "append style to template"() {
		def template = new Template("""
			<style>
			</style>
			/*style-insert-point*/
		""")

		assertThat(template.appendStyle("<style>css{}</style>").text, equalTo("""
			<style>
			</style>
			<style>css{}</style>/*style-insert-point*/
		"""))
	}

	@Test void "append script to template"() {
		def template = new Template("""
			<script/>
			/*script-insert-point*/
		""")

		assertThat(template.appendScript("<script>var i = 1;</script>").text, equalTo("""
			<script/>
			<script>var i = 1;</script>/*script-insert-point*/
		"""))
	}
}
