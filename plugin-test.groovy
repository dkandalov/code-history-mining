import analysis._private.Analysis
import analysis._private.AnalysisTest
import analysis._private.AnalysisUtilTest
import analysis._private.CombiningVisualizationTest
import historystorage.EventStorageTest
import vcsaccess._private.ChangeEventsReaderGitTest
import vcsaccess._private.CommitReaderGitTest
import vcsaccess._private.TextCompareProcessorTest
import vcsaccess.wilt.WiltTest
import analysis.templates.TemplateTest
import analysis.templates.TemplatesModificationTest
import liveplugin.testrunner.IntegrationTestsRunner
import util.CollectionUtilTest
import util.TimeIteratorsTest
// some classes to keep imports, without it groovy compilation fails
[Analysis.class]

def unitTests = [
		AnalysisUtilTest, AnalysisTest, EventStorageTest, TimeIteratorsTest, CombiningVisualizationTest,
		WiltTest, TemplateTest, TemplatesModificationTest, CollectionUtilTest
]
def integrationTests = [TextCompareProcessorTest, CommitReaderGitTest, ChangeEventsReaderGitTest]
IntegrationTestsRunner.runIntegrationTests(unitTests + integrationTests, project, pluginPath)
