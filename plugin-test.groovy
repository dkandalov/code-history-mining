import liveplugin.testrunner.IntegrationTestsRunner
import codemining.plugin.GroovyStubber
import codemining.plugin.CodeMiningPluginTest
import codemining.vcsaccess.implementation.MiningCommitReader_GitIntegrationTest
import codemining.vcsaccess.implementation.IJCommitReaderGitTest

// add-to-classpath $HOME/Library/Application Support/IntelliJIdea14/live-plugins/code-history-mining/build/classes/main/
// add-to-classpath $PLUGIN_PATH/lib/codemining/core/1.0/core-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/codemining/vcs-reader/1.0/vcs-reader-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/liveplugin/live-plugin/0.5.2 beta/live-plugin-0.5.2 beta.jar
// add-to-classpath $PLUGIN_PATH/lib/org/apache/commons/commons-csv/1.0/commons-csv-1.0.jar

def unitTests = [GroovyStubber, CodeMiningPluginTest]
def integrationTests = [IJCommitReaderGitTest, MiningCommitReader_GitIntegrationTest]
def tests = (unitTests + integrationTests).toList()
IntegrationTestsRunner.runIntegrationTests(tests, project, pluginPath)
