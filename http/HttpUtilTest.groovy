package http
import org.junit.Test

import static http.HttpUtil.fillTemplate
import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

class HttpUtilTest {
	@Test void "fill in project name in template"() {
		def templateText = "var name = /*project_name_placeholder*/\"some project\"/*project_name_placeholder*/;"
		assert fillTemplate(templateText, "myProject", anything) ==  "var name = \"myProject\";"
	}

	@Test void "fill in data in a template"() {
		def templateText = "var data = /*data_placeholder*/[1,2,3]/*data_placeholder*/;"
		assert fillTemplate(templateText, anything, "[5,6,7]") ==  "var data = [5,6,7];"
	}

	@Test void "inline javascript libraries in template"() {
		def templateText = """
			<body>
				<script src="lib1.js"></script>
				<script src="lib2.js"></script>
			</body>
		"""

		def fileReader = {
			if (it == "lib1.js") "// lib1 code"
			else if (it == "lib2.js") "// lib2 code"
		}

		assertThat(fillTemplate(templateText, anything, anything, fileReader), equalTo("""
			<body>
				<script>// lib1 code</script>
				<script>// lib2 code</script>
			</body>
		"""))
	}

	@Test void "inline css in template"() {
		def templateText = """
			<head>
				<stuff/>
				<link rel="stylesheet" type="text/css" media="screen" href="stylesheet1.css">
				<link rel="stylesheet" type="text/css" media="screen" href="stylesheet2.css">
			</head>
		"""

		def fileReader = {
			if (it == "stylesheet1.css") "css1 {}"
			else if (it == "stylesheet2.css") "css2 {}"
		}

		assertThat(fillTemplate(templateText, anything, anything, fileReader), equalTo("""
			<head>
				<stuff/><style>css1 {}</style><style>css2 {}</style></head>
		"""))
	}

	private static final anything = ""
}
