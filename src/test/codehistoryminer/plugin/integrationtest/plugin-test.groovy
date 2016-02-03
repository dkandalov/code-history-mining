package codehistoryminer.plugin.integrationtest
import codehistoryminer.plugin.historystorage.HistoryGrabberConfigTest
import codehistoryminer.plugin.vcsaccess.implementation.IJCommitReaderGitTest
import codehistoryminer.plugin.vcsaccess.implementation.MiningMachine_GitIntegrationTest
import liveplugin.testrunner.IntegrationTestsRunner

// add-to-classpath $PLUGIN_PATH/build/classes/main/
// add-to-classpath $PLUGIN_PATH/build/classes/test/
// add-to-classpath $HOME/IdeaProjects/code-history-miner/src/main/
// add-to-classpath $HOME/IdeaProjects/code-history-miner/build/classes/main/
// add-to-classpath $PLUGIN_PATH/lib/codehistoryminer/core/1.0/core-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/codehistoryminer/vcs-reader/1.0/vcs-reader-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/liveplugin/live-plugin/0.5.7 beta/live-plugin-0.5.7 beta.jar
// add-to-classpath $PLUGIN_PATH/lib/org/apache/commons/commons-csv/1.0/commons-csv-1.0.jar

def unitTests = [GroovyStubber, CodeHistoryMinerPluginTest, HistoryGrabberConfigTest]
def integrationTests = [IJCommitReaderGitTest, MiningMachine_GitIntegrationTest]
def tests = (unitTests + integrationTests).toList()
IntegrationTestsRunner.runIntegrationTests(tests, project, pluginPath)
