import com.intellij.openapi.project.Project
import intellijeval.PluginUtil
import org.junit.Test

class IntegrationTestsRunner {
	static def runIntegrationTests(Project project, List testClasses) {
		testClasses.each{ runTestsInClass(it, project) }
	}

	private static void runTestsInClass(Class testClass, Project project) {
		def testMethods = testClass.declaredMethods.findAll{ it.annotations.find{ it instanceof Test } != null }
		testMethods.each{ method ->
			runTestWithIDEConsoleOutput(method.name, testClass.simpleName, project){
				method.invoke(testClass.newInstance())
			}
		}
	}

	private static runTestWithIDEConsoleOutput(String methodName, String className, Project project, Closure closure) {
		try {

			closure()
			PluginUtil.showInConsole("${className}: ${methodName} - OK", "Integration tests", project)

		} catch (AssertionError assertionError) {
			def writer = new StringWriter()
			assertionError.printStackTrace(new PrintWriter(writer))
			PluginUtil.showInConsole(writer.buffer.toString(), className, project)
		}
	}
}
