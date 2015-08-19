package codehistoryminer.plugin.ui

import codehistoryminer.core.common.langutil.Date
import com.intellij.icons.AllIcons
import com.intellij.ide.ClipboardSynchronizer
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindowAnchor
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil

import javax.swing.*
import javax.swing.table.DefaultTableModel
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent

import static java.awt.GridBagConstraints.BOTH
import static java.awt.GridBagConstraints.NORTH
import static liveplugin.PluginUtil.*

class FileHistoryStatsToolWindow {
	private static final toolWindowId = "File History Stats"

	static showIn(Project project, Map fileHistoryStats) {
		def createToolWindowPanel = {
			JPanel rootPanel = new JPanel().with{
				layout = new GridBagLayout()

				def newLabel = { String title ->
					def label = new JLabel(title)
					label.background = UIUtil.labelBackground
					label.font = UIUtil.labelFont
					UIUtil.applyStyle(UIUtil.ComponentStyle.REGULAR, label)
					label
				}
				def newPanel = { Closure closure ->
					def panel = new JPanel()
					panel.layout = new GridBagLayout()
					def bag = new GridBag().setDefaultWeightX(1).setDefaultWeightY(1).setDefaultFill(BOTH)
					closure.resolveStrategy = DELEGATE_FIRST
					closure.delegate = panel
					closure.call(bag)
					panel
				}

				def infoPanel = newPanel { GridBag bag ->
					add(newLabel("Overall info"), bag.nextLine().next().fillCellHorizontally().weighty(0.01))
					def overallStats = [
							"File name": fileHistoryStats.virtualFile.name,
							"Creation date": DateFormatUtil.dateFormat.format((fileHistoryStats.creationDate as Date).javaDate()),
							"Amount of commits": fileHistoryStats.amountOfCommits,
							"File age in days": fileHistoryStats.fileAgeInDays
					]
					JBTable table = createTable(overallStats, ["", "Value"])
					add(new JBScrollPane(table), bag.nextLine().next().anchor(NORTH).weighty(0.2))
				}

				def authorsPanel = newPanel { GridBag bag ->
					add(newLabel(" "), bag.nextLine().next().fillCellHorizontally().weighty(0.01))
					add(newLabel("Amount of commits by author (top 10)"), bag.nextLine().next().fillCellHorizontally().weighty(0.01))
					JBTable table = createTable(fileHistoryStats.commitsAmountByAuthor, ["Author", "Commits"])
					add(new JBScrollPane(table), bag.nextLine().next().anchor(NORTH))
				}

				def prefixPanel = newPanel { GridBag bag ->
					add(newLabel(" "), bag.nextLine().next().fillCellHorizontally().weighty(0.01))
					add(newLabel("Amount of commits by message prefix (top 10)"), bag.nextLine().next().fillCellHorizontally().weighty(0.01))
					JBTable table = createTable(fileHistoryStats.commitsAmountByPrefix, ["Commit message prefix", "Commits"])
					add(new JBScrollPane(table), bag.nextLine().next().anchor(NORTH))
				}

				def splitter = new JBSplitter(true, 0.15 as float).with {
					firstComponent = infoPanel
					secondComponent = new JBSplitter(true).with {
						firstComponent = authorsPanel
						secondComponent = prefixPanel
						it
					}
					it
				}
				GridBag bag = new GridBag().setDefaultWeightX(1).setDefaultWeightY(1).setDefaultFill(BOTH)
				add(splitter, bag.nextLine().next().fillCell())

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

		def toolWindow = registerToolWindowIn(project, toolWindowId, createToolWindowPanel(), ToolWindowAnchor.RIGHT)
		def doNothing = {} as Runnable
		toolWindow.show(doNothing)
	}

	static showPlayground(Project project) {
		showIn(project, [
				virtualFile: currentFileIn(project),
				amountOfCommits: 123,
				creationDate: Date.today(),
				fileAgeInDays: 234,
				commitsAmountByAuthor: ["Me": 1],
				commitsAmountByPrefix: ["added": 1, "removed" :2]
		])
	}

	private static JBTable createTable(Map commitsAmountByPrefix, java.util.List<String> columns) {
		def tableModel = new DefaultTableModel() {
			@Override boolean isCellEditable(int row, int column) { false }
		}
		columns.each { tableModel.addColumn(it) }
		commitsAmountByPrefix.entrySet().each{ tableModel.addRow([it.key, it.value].toArray()) }

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
}
