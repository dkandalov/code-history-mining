import codemining.core.common.langutil.Measure
import codemining.historystorage.HistoryStorage
import codemining.plugin.CodeMiningPlugin
import codemining.plugin.Log
import codemining.plugin.ui.FileHistoryStatsToolWindow
import codemining.plugin.ui.UI
import codemining.vcsaccess.VcsActions
import com.intellij.openapi.application.PathManager
import liveplugin.PluginUtil

import static liveplugin.PluginUtil.show
// add-to-classpath $HOME/Library/Application Support/IntelliJIdea14/live-plugins/code-history-miner/src/main/
// add-to-classpath $HOME/Library/Application Support/IntelliJIdea14/live-plugins/code-history-miner/build/classes/main/
// add-to-classpath $PLUGIN_PATH/lib/codemining/core/1.0/core-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/codemining/vcs-reader/1.0/vcs-reader-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/liveplugin/live-plugin/0.5.2 beta/live-plugin-0.5.2 beta.jar
// add-to-classpath $PLUGIN_PATH/lib/org/apache/commons/commons-csv/1.0/commons-csv-1.0.jar

//noinspection GroovyConstantIfStatement
if (false) return FileHistoryStatsToolWindow.showPlayground(project)

def pathToHistoryFiles = "${PathManager.pluginsPath}/code-history-mining"

def log = new Log()
def measure = new Measure()

def storage = new HistoryStorage(pathToHistoryFiles, measure, log)
def vcsAccess = new VcsActions(measure, log)
def ui = new UI()
def miner = new CodeMiningPlugin(ui, storage, vcsAccess, measure, log)
ui.miner = miner
ui.storage = storage
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


