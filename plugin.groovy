import com.intellij.openapi.application.PathManager
import historystorage.HistoryStorage
import miner.Miner
import miner.UI
import util.Log
import util.Measure
import vcsaccess.CommitMunging_Playground
import vcsaccess.VcsAccess

import static liveplugin.PluginUtil.show

//noinspection GroovyConstantIfStatement
if (false) return CommitMunging_Playground.playOnIt()

def pathToHistoryFiles = "${PathManager.pluginsPath}/code-history-mining"

def log = new Log()
def measure = new Measure()

def storage = new HistoryStorage(pathToHistoryFiles, measure, log)
def vcsAccess = new VcsAccess(log)
def ui = new UI()
def miner = new Miner(ui, storage, vcsAccess, measure, log)
ui.miner = miner
ui.storage = storage
ui.log = log


if (!isIdeStartup) show("Reloaded code-history-mining plugin")


