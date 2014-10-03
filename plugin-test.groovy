import liveplugin.testrunner.IntegrationTestsRunner
import miner.GroovyStubber
import miner.MinerTest
import vcsaccess.implementation.ChangeEventsReaderGitTest
import vcsaccess.implementation.CommitReaderGitTest

// add-to-classpath $PLUGIN_PATH/lib/vcs-reader.jar
// add-to-classpath $PLUGIN_PATH/lib/code-mining-core.jar

def unitTests = [GroovyStubber, MinerTest]
def integrationTests = [CommitReaderGitTest, ChangeEventsReaderGitTest]
def tests = (unitTests + integrationTests).toList()
IntegrationTestsRunner.runIntegrationTests(tests, project, pluginPath)
