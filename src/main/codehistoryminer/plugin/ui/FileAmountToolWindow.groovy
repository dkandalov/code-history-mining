package codehistoryminer.plugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.ClipboardSynchronizer
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.NotNull

import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent

import static com.intellij.openapi.wm.ToolWindowAnchor.RIGHT
import static java.awt.GridBagConstraints.*
import static liveplugin.PluginUtil.unregisterToolWindow

class FileAmountToolWindow {
	private static final toolWindowId = "File Amount by Type"

	static showIn(Project project, Map fileCountByFileExtension) {
		def createToolWindowPanel = {
			JPanel rootPanel = new JPanel().with{
				layout = new GridBagLayout()
				GridBag bag = new GridBag().setDefaultWeightX(1).setDefaultWeightY(1).setDefaultFill(BOTH)

				def totalAmountOfFiles = fileCountByFileExtension.entrySet().sum(0){ it.value }
				JBTable table = createTable(fileCountByFileExtension, totalAmountOfFiles)
				add(new JBScrollPane(table), bag.nextLine().next().anchor(NORTH))

				add(new JPanel().with {
					layout = new GridBagLayout()
					add(new JTextArea("(Please note that amount of files is based on IDE index and only shows file types IDE knows about.)").with{
						editable = false
						lineWrap = true
						wrapStyleWord = true
						background = UIUtil.labelBackground
                        font = UIUtil.labelFont
						UIUtil.applyStyle(UIUtil.ComponentStyle.REGULAR, it)
						it
					}, new GridBag().setDefaultWeightX(1).setDefaultWeightY(1).nextLine().next().fillCellHorizontally().anchor(NORTH))
					it
				}, bag.nextLine().next().anchor(SOUTH))
				it
			}

			def actionGroup = new DefaultActionGroup().with{
				add(new AnAction(AllIcons.Actions.Cancel) {
					@Override void actionPerformed(AnActionEvent event) {
						unregisterToolWindow(toolWindowId)
					}
				})
				it
			}

			def toolWindowPanel = new SimpleToolWindowPanel(true)
			toolWindowPanel.content = rootPanel
			toolWindowPanel.toolbar = ActionManager.instance.createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, actionGroup, true).component
			toolWindowPanel
		}

		def toolWindow = registerToolWindowIn(project, toolWindowId, createToolWindowPanel(), RIGHT)
		toolWindow.show({} as Runnable)
	}

	private static JBTable createTable(fileCountByFileExtension, totalAmountOfFiles) {
		def tableModel = new DefaultTableModel() {
			@Override boolean isCellEditable(int row, int column) { false }
		}
		tableModel.addColumn("File extension")
		tableModel.addColumn("Amount")
		fileCountByFileExtension.entrySet().each{
			tableModel.addRow([it.key, it.value].toArray())
		}
		tableModel.addRow(["Total", totalAmountOfFiles].toArray())
		def table = new JBTable(tableModel).with{
			striped = true
			showGrid = false
			it
		}
		registerCopyToClipboardShortCut(table, tableModel)
		table
	}

	private static registerCopyToClipboardShortCut(JTable table, DefaultTableModel tableModel) {
		KeyStroke copyKeyStroke = KeymapUtil.getKeyStroke(ActionManager.instance.getAction(IdeActions.ACTION_COPY).shortcutSet)
		table.registerKeyboardAction(new AbstractAction() {
			@Override void actionPerformed(ActionEvent event) {
				def selectedCells = table.selectedRows.collect{ row ->
					(0..<tableModel.columnCount).collect{ column ->
						tableModel.getValueAt(row, column).toString() }
				}
				def content = new StringSelection(selectedCells.collect{ it.join(",") }.join("\n"))
				ClipboardSynchronizer.instance.setContent(content, content)
			}
		}, "Copy", copyKeyStroke, JComponent.WHEN_FOCUSED)
	}

	private static ToolWindow registerToolWindowIn(@NotNull Project project, String toolWindowId,
	                                               JComponent component, ToolWindowAnchor location = RIGHT) {
		def manager = ToolWindowManager.getInstance(project)
		if (manager.getToolWindow(toolWindowId) != null) {
			manager.unregisterToolWindow(toolWindowId)
		}

		def toolWindow = manager.registerToolWindow(toolWindowId, false, location)
		def content = ContentFactory.SERVICE.instance.createContent(component, "", false)
		toolWindow.contentManager.addContent(content)
		toolWindow
	}
}
