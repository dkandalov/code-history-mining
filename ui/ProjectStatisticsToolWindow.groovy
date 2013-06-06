package ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.GridBag

import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.*

import static intellijeval.PluginUtil.registerToolWindowIn
import static intellijeval.PluginUtil.unregisterToolWindow
import static java.awt.GridBagConstraints.*

class ProjectStatisticsToolWindow {
	static showIn(project, fileCountByFileExtension) {
		def totalAmountOfFiles = fileCountByFileExtension.entrySet().sum(0){ it.value }

		def actionGroup = new DefaultActionGroup().with{
			add(new AnAction(AllIcons.Actions.Cancel) {
				@Override void actionPerformed(AnActionEvent event) {
					unregisterToolWindow("Project Statistics")
				}
			})
			it
		}

		def createToolWindowPanel = {
			JPanel rootPanel = new JPanel().with{
				def tableModel = new DefaultTableModel() {
					@Override boolean isCellEditable(int row, int column) { false }
				}
				tableModel.addColumn("File extension")
				tableModel.addColumn("File count")
				fileCountByFileExtension.entrySet().each {
					tableModel.addRow([it.key, it.value].toArray())
				}
				tableModel.addRow(["Total", totalAmountOfFiles].toArray())
				def table = new JBTable(tableModel).with { // TODO make table content copyable
					striped = true
					showGrid = false
					it
				}

				layout = new GridBagLayout()
				GridBag bag = new GridBag().setDefaultWeightX(1).setDefaultWeightY(1).setDefaultFill(BOTH)
				add(new JBScrollPane(table), bag.nextLine().next().anchor(NORTH))

				it
			}

			def toolWindowPanel = new SimpleToolWindowPanel(true)
			toolWindowPanel.content = rootPanel
			toolWindowPanel.toolbar = ActionManager.instance.createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, actionGroup, true).component
			toolWindowPanel
		}

		def toolWindow = registerToolWindowIn(project, "Project Statistics", createToolWindowPanel(), ToolWindowAnchor.RIGHT)
		def doNothing = {} as Runnable
		toolWindow.show(doNothing)
	}
}
