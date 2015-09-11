import codehistoryminer.core.common.langutil.Measure
import codehistoryminer.historystorage.HistoryStorage
import codehistoryminer.historystorage.QueryScriptsStorage
import codehistoryminer.plugin.CodeHistoryMinerPlugin
import codehistoryminer.plugin.Log
import codehistoryminer.plugin.ui.FileHistoryStatsToolWindow
import codehistoryminer.plugin.ui.UI
import codehistoryminer.vcsaccess.VcsActions
import com.intellij.openapi.application.PathManager
import liveplugin.PluginUtil

import static liveplugin.PluginUtil.show
// add-to-classpath $HOME/Library/Application Support/IntelliJIdea15/live-plugins/code-history-miner/src/main/
// add-to-classpath $HOME/Library/Application Support/IntelliJIdea15/live-plugins/code-history-miner/build/classes/main/
// add-to-classpath $PLUGIN_PATH/lib/codehistoryminer/core/1.0/core-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/codehistoryminer/vcs-reader/1.0/vcs-reader-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/liveplugin/live-plugin/0.5.4 beta/live-plugin-0.5.4 beta.jar
// add-to-classpath $PLUGIN_PATH/lib/org/apache/commons/commons-csv/1.0/commons-csv-1.0.jar

//noinspection GroovyConstantIfStatement
if (false) return FileHistoryStatsToolWindow.showPlayground(project)

def pathToHistoryFiles = "${PathManager.pluginsPath}/code-history-mining"
def pathToQueryFiles = "${PathManager.pluginsPath}/code-history-mining/query-scripts"

def log = new Log()
def measure = new Measure()

def historyStorage = new HistoryStorage(pathToHistoryFiles, measure, log)
def scriptsStorage = new QueryScriptsStorage(pathToQueryFiles)
def vcsAccess = new VcsActions(measure, log)
def ui = new UI()
def miner = new CodeHistoryMinerPlugin(ui, historyStorage, scriptsStorage, vcsAccess, measure, log)
ui.miner = miner
ui.storage = historyStorage
ui.log = log
ui.init()


// this code below is only useful for reloading in live-plugin
PluginUtil.changeGlobalVar("CodeHistoryMiningState"){ oldState ->
	if (oldState != null) {
		ui.dispose(oldState.ui)
		vcsAccess.dispose(oldState.vcsAccess)
	}
	[ui: ui, vcsAccess: vcsAccess]
}
if (!isIdeStartup) show("Reloaded code-history-mining plugin")


