import liveplugin.testrunner.IntegrationTestsRunner
import codemining.plugin.GroovyStubber
import codemining.plugin.MinerTest
import codemining.vcsaccess.implementation.ChangeEventsReaderGitTest
import codemining.vcsaccess.implementation.CommitReaderGitTest

// add-to-classpath $PLUGIN_PATH/lib/vcs-reader.jar
// add-to-classpath $PLUGIN_PATH/lib/code-mining-core.jar
// add-to-classpath $PLUGIN_PATH/lib/commons-csv-1.0.jar

def unitTests = [GroovyStubber, MinerTest]
def integrationTests = [CommitReaderGitTest, ChangeEventsReaderGitTest]
def tests = (unitTests + integrationTests).toList()
IntegrationTestsRunner.runIntegrationTests(tests, project, pluginPath)
