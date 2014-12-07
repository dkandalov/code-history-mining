import liveplugin.testrunner.IntegrationTestsRunner
import codemining.plugin.GroovyStubber
import codemining.plugin.CodeMiningPluginTest
import codemining.vcsaccess.implementation.MiningCommitReader_GitIntegrationTest
import codemining.vcsaccess.implementation.IJCommitReaderGitTest

// add-to-classpath $HOME/Library/Application Support/IntelliJIdea14/live-plugins/code-history-mining/build/classes/main/
// add-to-classpath $PLUGIN_PATH/lib/vcs-reader.jar
// add-to-classpath $PLUGIN_PATH/lib/code-mining-core.jar
// add-to-classpath $PLUGIN_PATH/lib/commons-csv-1.0.jar

def unitTests = [GroovyStubber, CodeMiningPluginTest]
def integrationTests = [IJCommitReaderGitTest, MiningCommitReader_GitIntegrationTest]
def tests = (unitTests + integrationTests).toList()
IntegrationTestsRunner.runIntegrationTests(tests, project, pluginPath)
