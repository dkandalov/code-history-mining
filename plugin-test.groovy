import analysis.Analysis
import analysis.AnalysisTest
import events.EventStorageTest
import historyreader.ChangeEventsReaderGitTest
import historyreader.CommitReaderGitTest
import historyreader.TextCompareProcessorTest
import historyreader.wilt.WiltTest
import http.TemplateTest
import liveplugin.testrunner.IntegrationTestsRunner
import util.TimeIteratorsTest

def unitTests = [AnalysisTest, EventStorageTest, TimeIteratorsTest, WiltTest, TemplateTest]
def integrationTests = [TextCompareProcessorTest, CommitReaderGitTest, ChangeEventsReaderGitTest]
IntegrationTestsRunner.runIntegrationTests(unitTests /*+ integrationTests*/, project, pluginPath)
