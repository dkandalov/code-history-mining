import analysis.Analysis
import analysis.AnalysisTest
import events.EventStorageTest
import historyreader.ChangeEventsReaderGitTest
import historyreader.CommitReaderGitTest
import historyreader.TextCompareProcessorTest
import historyreader.wilt.WiltTest
import http.TemplateTest
import http.TemplatesModificationTest
import liveplugin.testrunner.IntegrationTestsRunner
import util.TimeIteratorsTest

// added to keep "import analysis.Analysis", without it groovy compilation fails
[Analysis.class]

/* CombiningVisualizationTest <- importing it causes groovy compilation error in live plugin but not in standalone groovy */
def unitTests = [AnalysisTest, EventStorageTest, TimeIteratorsTest, WiltTest, TemplateTest, TemplatesModificationTest]
def integrationTests = [TextCompareProcessorTest, CommitReaderGitTest, ChangeEventsReaderGitTest]
IntegrationTestsRunner.runIntegrationTests(unitTests /*+ integrationTests*/, project, pluginPath)
