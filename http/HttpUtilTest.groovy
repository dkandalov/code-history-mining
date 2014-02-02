package http

import org.junit.Test

import static http.HttpUtil.fillTemplate

class HttpUtilTest {
	@Test void "fill in project name in a template"() {
		def templateText = "var name = /*project_name_placeholder*/\"some project\"/*project_name_placeholder*/;"
		assert fillTemplate(templateText, "myProject", anything) ==  "var name = \"myProject\";"
	}

	@Test void "fill in data in a template"() {
		def templateText = "var data = /*data_placeholder*/[1,2,3]/*data_placeholder*/;"
		assert fillTemplate(templateText, anything, "[5,6,7]") ==  "var data = [5,6,7];"
	}

	@Test void "inline javascript libraries in a template"() {
		def templateText = """
			<script src="lib1.js"></script>
			<script src="lib2.js"></script>
		"""

		def fileReader = {
			if (it == "lib1.js") "// lib1 code"
			else if (it == "lib2.js") "// lib2 code"
		}

		assert fillTemplate(templateText, anything, anything, fileReader) ==  """
			<script>// lib1 code</script>
			<script>// lib2 code</script>
		"""
	}

	private static final anything = ""
}
