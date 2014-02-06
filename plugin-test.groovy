import analysis._private.Analysis
import analysis._private.AnalysisTest
import analysis._private.AnalysisUtilTest
import events.EventStorageTest
import historyreader._private.ChangeEventsReaderGitTest
import historyreader._private.CommitReaderGitTest
import historyreader._private.TextCompareProcessorTest
import historyreader.wilt.WiltTest
import http.TemplateTest
import http.TemplatesModificationTest
import liveplugin.testrunner.IntegrationTestsRunner
import util.CollectionUtilTest
import util.TimeIteratorsTest
// some classes to keep imports, without it groovy compilation fails
[Analysis.class]

/* CombiningVisualizationTest <- importing it causes groovy compilation error in live plugin but not in standalone groovy */
def unitTests = [
		AnalysisUtilTest, AnalysisTest, EventStorageTest, TimeIteratorsTest,
		WiltTest, TemplateTest, TemplatesModificationTest, CollectionUtilTest
]
def integrationTests = [TextCompareProcessorTest, CommitReaderGitTest, ChangeEventsReaderGitTest]
IntegrationTestsRunner.runIntegrationTests(unitTests + integrationTests, project, pluginPath)
