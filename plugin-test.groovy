import analysis.Analysis
import analysis.AnalysisTest
import events.EventStorageTest
import historyreader._private.ChangeEventsReaderGitTest
import historyreader._private.CommitReaderGitTest
import historyreader._private.TextCompareProcessorTest
import historyreader.wilt.WiltTest
import http.TemplateTest
import http.TemplatesModificationTest
import liveplugin.testrunner.IntegrationTestsRunner
import util.TimeIteratorsTest
// some classes to keep imports, without it groovy compilation fails
[Analysis.class]

/* CombiningVisualizationTest <- importing it causes groovy compilation error in live plugin but not in standalone groovy */
def unitTests = [AnalysisTest, EventStorageTest, TimeIteratorsTest, WiltTest, TemplateTest, TemplatesModificationTest]
def integrationTests = [TextCompareProcessorTest, CommitReaderGitTest, ChangeEventsReaderGitTest]
IntegrationTestsRunner.runIntegrationTests(unitTests + integrationTests, project, pluginPath)
