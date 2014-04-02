import analysis._private.Analysis
import analysis._private.AnalysisTest
import analysis._private.AnalysisUtilTest
import analysis._private.CombiningVisualizationTest
import analysis.templates.TemplateTest
import analysis.templates.TemplatesModificationTest
import historystorage.EventStorageTest
import liveplugin.testrunner.IntegrationTestsRunner
import miner.MinerTest
import common.langutil.CollectionUtilTest
import common.langutil.GroovyStubber
import common.langutil.TimeIteratorsTest
import vcsaccess.implementation.ChangeEventsReaderGitTest
import vcsaccess.implementation.CommitReaderGitTest
import vcsaccess.implementation.TextCompareProcessorTest
import vcsaccess.implementation.wilt.WiltTest

// some classes to keep imports, without it groovy compilation fails
[Analysis.class]

def unitTests = [
		AnalysisUtilTest, AnalysisTest, EventStorageTest, TimeIteratorsTest, CombiningVisualizationTest,
		WiltTest, TemplateTest, TemplatesModificationTest, CollectionUtilTest, MinerTest, GroovyStubber
]
def integrationTests = [TextCompareProcessorTest, CommitReaderGitTest, ChangeEventsReaderGitTest]
IntegrationTestsRunner.runIntegrationTests(unitTests + integrationTests, project, pluginPath)
