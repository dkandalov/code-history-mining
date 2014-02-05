package ui
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.DocumentAdapter
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.GridBag
import com.michaelbaranov.microba.calendar.DatePicker
import events.EventStorage
import historyreader.HistoryGrabberConfig

import javax.swing.*
import javax.swing.event.DocumentEvent
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static java.awt.GridBagConstraints.HORIZONTAL

class Dialog {
	static showDialog(HistoryGrabberConfig grabberConfig, String dialogTitle, Project project, Closure onOkCallback) {
		def fromDatePicker = new DatePicker(grabberConfig.from, dateFormat.delegate)
		fromDatePicker.focusLostBehavior = JFormattedTextField.COMMIT
		def toDatePicker = new DatePicker(grabberConfig.to, dateFormat.delegate)
		toDatePicker.focusLostBehavior = JFormattedTextField.COMMIT
		def filePathTextField = new TextFieldWithBrowseButton()
		filePathTextField.text = grabberConfig.outputFilePath
		def grabChangeSizeCheckBox = new JCheckBox()
		grabChangeSizeCheckBox.selected = grabberConfig.grabChangeSizeInLines
		def historyRangeLabel = new JLabel()

		grabChangeSizeCheckBox.toolTipText = "Requires loading files content. Can slow down history grabbing."

		JPanel rootPanel = new JPanel().with{
			layout = new GridBagLayout()
			GridBag bag = new GridBag().setDefaultFill(HORIZONTAL)
			bag.defaultInsets = new Insets(5, 5, 5, 5)

			add(new JLabel("From:"), bag.nextLine().next())
			add(fromDatePicker, bag.next())
			add(new JLabel("To:"), bag.next())
			add(toDatePicker, bag.next())
			add(new JLabel(), bag.next().fillCellHorizontally())
			add(new JLabel("Save to:"), bag.nextLine().next())
			def actionListener = new ActionListener() {
				@Override void actionPerformed(ActionEvent e) {
					def csvFileType = FileTypeManager.instance.getFileTypeByExtension("csv")
					VirtualFile file = FileChooser.chooseFile(fileChooserDescriptor(csvFileType), project, VirtualFileManager.instance.findFileByUrl("file://" + filePathTextField.text))
					if (file != null) {
						filePathTextField.text = file.path
						updateDateRangeText(historyRangeLabel, filePathTextField)
					}
				}
			}
			filePathTextField.addActionListener(actionListener)
			filePathTextField.childComponent.document.addDocumentListener(new DocumentAdapter() {
				@Override protected void textChanged(DocumentEvent e) {
					updateDateRangeText(historyRangeLabel, filePathTextField)
				}
			})
			add(filePathTextField, bag.next().coverLine().weightx(1).fillCellHorizontally())
			add(historyRangeLabel, bag.nextLine().next().next().coverLine())

			add(new JPanel().with {
				layout = new GridBagLayout()
				GridBag bag2 = new GridBag()
				add(new JLabel("Grab change size in lines/characters:"), bag2.nextLine().next())
				add(grabChangeSizeCheckBox, bag2.next().coverLine().weightx(1).fillCellHorizontally())
				it
			}, bag.nextLine().coverLine())

			def text = new JLabel("(Please note that grabbing history can slow down IDE and/or take a really long time.)")
			add(text, bag.nextLine().coverLine())

			updateDateRangeText(historyRangeLabel, filePathTextField)
			it
		}

		DialogBuilder builder = new DialogBuilder(project)
		builder.title = dialogTitle
		builder.okActionEnabled = true
		builder.okOperation = {
			onOkCallback(new HistoryGrabberConfig(
					fromDatePicker.date,
					toDatePicker.date,
					filePathTextField.text,
					grabChangeSizeCheckBox.selected
			))
			builder.dialogWrapper.close(0)
		} as Runnable
		builder.centerPanel = rootPanel
		builder.dimensionServiceKey = "CodeHistoryMiningDialog"

		ApplicationManager.application.invokeLater{ builder.showModal(true) } as Runnable
	}

	private static updateDateRangeText(JLabel label, TextFieldWithBrowseButton filePathTextField) {
		def file = new File(filePathTextField.text)
		if (!file.exists()) {
			label.text = "(contains no history)"
		} else {
			def eventStorage = new EventStorage(file.absolutePath)
			def fromDate = DateFormatUtil.formatDate(eventStorage.oldestEventTime)
			def toDate = DateFormatUtil.formatDate(eventStorage.mostRecentEventTime)
			label.text = "(contains history from ${fromDate} to ${toDate})"
		}
	}

	private static FileChooserDescriptor fileChooserDescriptor(FileType csvFileType) {
		FileChooserDescriptorFactory.createSingleFileDescriptor(csvFileType).with{
			showFileSystemRoots = true
			title = "Output File"
			description = "Select output file"
			hideIgnored = false
			it
		}
	}

}
