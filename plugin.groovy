import com.intellij.openapi.application.PathManager
import util.Log
import vcsaccess.*
import historystorage.HistoryStorage
import miner.Miner
import ui.UI

import static liveplugin.PluginUtil.*

//noinspection GroovyConstantIfStatement
if (false) return CommitMunging_Playground.playOnIt()

def pathToHistoryFiles = "${PathManager.pluginsPath}/code-history-mining"

def log = new Log()

def storage = new HistoryStorage(pathToHistoryFiles, log)
def vcsAccess = new VcsAccess()
def ui = new UI()
def miner = new Miner(ui, storage, vcsAccess, log)
ui.miner = miner
ui.storage = storage


if (!isIdeStartup) show("Reloaded code-history-mining plugin")


