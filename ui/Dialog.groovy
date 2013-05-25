package ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.ui.GridBag
import com.michaelbaranov.microba.calendar.DatePicker

import javax.swing.*
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static java.awt.GridBagConstraints.*

class Dialog {
	static showDialog(DialogState state, String dialogTitle, Project project, Closure onOkCallback) {
		def fromDatePicker = new DatePicker(state.from, dateFormat.delegate)
		def toDatePicker = new DatePicker(state.to, dateFormat.delegate)
		def vcsRequestSizeField = new JTextField(String.valueOf(state.vcsRequestBatchSizeInDays))
		def filePathTextField = new TextFieldWithBrowseButton()

		JPanel rootPanel = new JPanel().with{
			layout = new GridBagLayout()
			GridBag bag = new GridBag()
					.setDefaultAnchor(0, EAST)
					.setDefaultAnchor(1, WEST)
					.setDefaultWeightX(1, 1)
					.setDefaultFill(HORIZONTAL)
			bag.defaultInsets = new Insets(5, 5, 5, 5)

			add(new JLabel("From:"), bag.nextLine().next())
			add(fromDatePicker, bag.next())
			add(new JLabel("To:"), bag.next())
			add(toDatePicker, bag.next())
			add(new JLabel("VCS Request batch size:"), bag.nextLine().next())
			add(vcsRequestSizeField, bag.next())
			add(new JLabel("day(s)"), bag.next().coverLine())
			add(new JLabel("File path:"), bag.nextLine().next())
			def actionListener = new ActionListener() {
				@Override void actionPerformed(ActionEvent e) {
					def csvFileType = FileTypeManager.instance.getFileTypeByExtension("csv")
					FileChooserDescriptor chooserDescriptor = FileChooserDescriptorFactory.createSingleFileDescriptor(csvFileType).with{
						showFileSystemRoots = true
						title = "Output File"
						description = "Select output file"
						hideIgnored = false
						it
					}
					VirtualFile file = FileChooser.chooseFile(chooserDescriptor, project, VirtualFileManager.instance.findFileByUrl("file://" + filePathTextField.text))
					if (file != null) filePathTextField.text = file.path
				}
			}
			filePathTextField.addActionListener(actionListener)
			filePathTextField.text = state.outputFilePath
			add(filePathTextField, bag.next().coverLine())


			def text = "(Note that grabbing history might significantly slow down UI and/or take a really long time for a big project)"
			def textArea = new JTextArea(text).with {
				lineWrap = true
				wrapStyleWord = true
				editable = false
				it
			}
			textArea.background = background
			add(textArea, bag.nextLine().coverLine())
			it
		}

		DialogBuilder builder = new DialogBuilder(project)
		builder.title = dialogTitle
		builder.okActionEnabled = true
		builder.okOperation = {
			def toInteger = {
				String s = it.replaceAll("\\D", "")
				s.empty ? 1 : s.toInteger()
			}
			onOkCallback(new DialogState(fromDatePicker.date, toDatePicker.date, toInteger(vcsRequestSizeField.text), filePathTextField.text))
			builder.dialogWrapper.close(0)
		} as Runnable
		builder.centerPanel = rootPanel

		ApplicationManager.application.invokeLater{ builder.showModal(true) } as Runnable
	}

}
