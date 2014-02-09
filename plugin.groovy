import com.intellij.openapi.application.PathManager
import vcsaccess.*
import historystorage.HistoryStorage
import miner.Miner
import ui.UI

import static liveplugin.PluginUtil.*

//noinspection GroovyConstantIfStatement
if (false) return CommitMunging_Playground.playOnIt()

def pathToHistoryFiles = "${PathManager.pluginsPath}/code-history-mining"

def storage = new HistoryStorage(pathToHistoryFiles)
def vcsAccess = new VcsAccess()
def ui = new UI()
def miner = new Miner(ui, storage, vcsAccess)
ui.miner = miner
ui.storage = storage


if (!isIdeStartup) show("Reloaded code-history-mining plugin")


