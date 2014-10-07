import liveplugin.testrunner.IntegrationTestsRunner
import miner.GroovyStubber
import miner.MinerTest
import vcsaccess.implementation.ChangeEventsReader2GitTest
import vcsaccess.implementation.CommitReaderGitTest

// add-to-classpath $PLUGIN_PATH/lib/vcs-reader.jar
// add-to-classpath $PLUGIN_PATH/lib/code-mining-core.jar

def unitTests = [GroovyStubber, MinerTest]
def integrationTests = [CommitReaderGitTest, ChangeEventsReader2GitTest]
def tests = (unitTests + integrationTests).toList()
IntegrationTestsRunner.runIntegrationTests(tests, project, pluginPath)
