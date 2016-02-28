package codehistoryminer.plugin

import codehistoryminer.core.lang.Measure
import codehistoryminer.plugin.historystorage.HistoryStorage
import codehistoryminer.plugin.historystorage.ScriptStorage
import codehistoryminer.plugin.ui.FileHistoryStatsToolWindow
import codehistoryminer.plugin.ui.UI
import codehistoryminer.plugin.vcsaccess.VcsActions
import com.intellij.openapi.application.PathManager
import liveplugin.PluginUtil

import static liveplugin.PluginUtil.show
// add-to-classpath $HOME/IdeaProjects/code-history-miner/src/main/
// add-to-classpath $HOME/IdeaProjects/code-history-miner/build/classes/main/
// add-to-classpath $PLUGIN_PATH/build/classes/main/
// add-to-classpath $PLUGIN_PATH/lib/codehistoryminer/core/1.0/core-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/codehistoryminer/vcs-reader/1.0/vcs-reader-1.0.jar
// add-to-classpath $PLUGIN_PATH/lib/liveplugin/live-plugin/0.5.7_beta/live-plugin-0.5.7_beta.jar
// add-to-classpath $PLUGIN_PATH/lib/org/apache/commons/commons-csv/1.0/commons-csv-1.0.jar

//noinspection GroovyConstantIfStatement
if (false) return FileHistoryStatsToolWindow.showPlayground(project)

def pathToHistoryFiles = "${PathManager.pluginsPath}/code-history-mining"
def pathToScriptFiles = "${PathManager.pluginsPath}/code-history-mining/scripts"

def log = new Log()
def measure = new Measure()

def historyStorage = new HistoryStorage(pathToHistoryFiles, measure, log)
def scriptStorage = new ScriptStorage(pathToScriptFiles)
def vcsAccess = new VcsActions(measure, log)
def ui = new UI()
def minerPlugin = new CodeHistoryMinerPlugin(ui, historyStorage, scriptStorage, vcsAccess, measure, log)
ui.init(minerPlugin, historyStorage, log)


// this code below is only useful for reloading in live-plugin
PluginUtil.changeGlobalVar("CodeHistoryMiningState"){ oldState ->
	if (oldState != null) {
		ui.dispose(oldState.ui)
		vcsAccess.dispose(oldState.vcsAccess)
	}
	[ui: ui, vcsAccess: vcsAccess]
}
if (!isIdeStartup) show("Reloaded code-history-mining plugin")


