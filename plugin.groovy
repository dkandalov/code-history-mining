import com.intellij.openapi.application.PathManager
import historystorage.HistoryStorage
import liveplugin.PluginUtil
import miner.Miner
import miner.ui.UI
import util.Log
import util.Measure
import vcsaccess.implementation.CommitMunging_Playground
import vcsaccess.VcsAccess

import static liveplugin.PluginUtil.show

//noinspection GroovyConstantIfStatement
if (false) return CommitMunging_Playground.playOnIt()

def pathToHistoryFiles = "${PathManager.pluginsPath}/code-history-mining"

def log = new Log()
def measure = new Measure()

def storage = new HistoryStorage(pathToHistoryFiles, measure, log)
def vcsAccess = new VcsAccess(measure, log)
def ui = new UI()
def miner = new Miner(ui, storage, vcsAccess, measure, log)
ui.miner = miner
ui.storage = storage
ui.log = log
ui.init()


// this is only useful for reloading this plugin in live-plugin
PluginUtil.changeGlobalVar("CodeHistoryMiningState"){ oldState ->
	if (oldState != null) {
		ui.dispose(oldState.ui)
		vcsAccess.dispose(oldState.vcsAccess)
	}
	[ui: ui, vcsAccess: vcsAccess]
}
if (!isIdeStartup) show("Reloaded code-history-mining plugin")


