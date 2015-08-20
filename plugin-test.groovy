import codehistoryminer.historystorage.HistoryGrabberConfigTest
import codehistoryminer.plugin.CodeHistoryMinerPluginTest
import codehistoryminer.plugin.GroovyStubber
import codehistoryminer.vcsaccess.implementation.IJCommitReaderGitTest
import codehistoryminer.vcsaccess.implementation.MiningCommitReader_GitIntegrationTest
import liveplugin.testrunner.IntegrationTestsTextRunner
// add-to-classpath $HOME/Library/Application Support/IntelliJIdea15/live-plugins/code-history-miner/build/classes/main/
// add-to-classpath $PLUGIN_PATH/lib/codehistoryminer/core/1.0/core-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/codehistoryminer/vcs-reader/1.0/vcs-reader-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/liveplugin/live-plugin/0.5.4 beta/live-plugin-0.5.4 beta.jar
// add-to-classpath $PLUGIN_PATH/lib/org/apache/commons/commons-csv/1.0/commons-csv-1.0.jar

def unitTests = [GroovyStubber, CodeHistoryMinerPluginTest, HistoryGrabberConfigTest]
def integrationTests = [IJCommitReaderGitTest, MiningCommitReader_GitIntegrationTest]
def tests = (unitTests + integrationTests).toList()
IntegrationTestsTextRunner.runIntegrationTests(tests, project, pluginPath)
