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
import com.intellij.util.ui.GridBag
import com.michaelbaranov.microba.calendar.DatePicker
import vcsaccess.HistoryGrabberConfig

import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static java.awt.GridBagConstraints.HORIZONTAL

class Dialog {
	static showDialog(HistoryGrabberConfig grabberConfig, String dialogTitle, Project project, Closure onOkCallback) {
		def fromDatePicker = new DatePicker(grabberConfig.from, dateFormat.delegate)
		def toDatePicker = new DatePicker(grabberConfig.to, dateFormat.delegate)
		def filePathTextField = new TextFieldWithBrowseButton()
		def grabChangeSizeCheckBox = new JCheckBox()
		def grabOnVcsUpdateCheckBox = new JCheckBox()

		fromDatePicker.focusLostBehavior = JFormattedTextField.COMMIT
		toDatePicker.focusLostBehavior = JFormattedTextField.COMMIT
		filePathTextField.text = grabberConfig.outputFilePath
		filePathTextField.addActionListener(onChooseFileAction(project, filePathTextField))
		grabChangeSizeCheckBox.selected = grabberConfig.grabChangeSizeInLines
		grabChangeSizeCheckBox.toolTipText = "Requires loading files content. Can slow down history grabbing."
		grabOnVcsUpdateCheckBox.selected = grabberConfig.grabOnVcsUpdate
		grabOnVcsUpdateCheckBox.addActionListener({ onGrabOnVcsUpdate(toDatePicker, grabOnVcsUpdateCheckBox) } as ActionListener)
		onGrabOnVcsUpdate(toDatePicker, grabOnVcsUpdateCheckBox)

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
			add(filePathTextField, bag.next().coverLine().weightx(1).fillCellHorizontally())

			add(new JPanel().with {
				layout = new GridBagLayout()
				GridBag bag2 = new GridBag()
				add(new JLabel("Grab history on VCS update:"), bag2.nextLine().next())
				add(grabOnVcsUpdateCheckBox, bag2.next().coverLine().weightx(1).fillCellHorizontally())
				it
			}, bag.nextLine().coverLine())

			add(new JPanel().with {
				layout = new GridBagLayout()
				GridBag bag2 = new GridBag()
				add(new JLabel("Grab change size in lines/characters:"), bag2.nextLine().next())
				add(grabChangeSizeCheckBox, bag2.next().coverLine().weightx(1).fillCellHorizontally())
				it
			}, bag.nextLine().coverLine())

			def text = new JLabel("(Please note that grabbing history can slow down IDE and/or take a really long time.)")
			add(text, bag.nextLine().coverLine())

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
					grabChangeSizeCheckBox.selected,
					grabOnVcsUpdateCheckBox.selected
			))
			builder.dialogWrapper.close(0)
		} as Runnable
		builder.centerPanel = rootPanel
		builder.dimensionServiceKey = "CodeHistoryMiningDialog"

		ApplicationManager.application.invokeLater{ builder.showModal(true) } as Runnable
	}

	private static onGrabOnVcsUpdate(toDatePicker, grabOnVcsUpdateCheckBox) {
		toDatePicker.enabled = !grabOnVcsUpdateCheckBox.selected
	}

	private static ActionListener onChooseFileAction(Project project, TextFieldWithBrowseButton filePathTextField) {
		new ActionListener() {
			@Override void actionPerformed(ActionEvent event) {
				def csvFileType = FileTypeManager.instance.getFileTypeByExtension("csv")
				VirtualFile file = FileChooser.chooseFile(
						fileChooserDescriptor(csvFileType),
						project,
						VirtualFileManager.instance.findFileByUrl("file://" + filePathTextField.text)
				)
				if (file != null) filePathTextField.text = file.path
			}
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
