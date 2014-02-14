package ui
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.ui.DocumentAdapter
import com.intellij.util.ui.GridBag
import com.michaelbaranov.microba.calendar.DatePicker
import vcsaccess.HistoryGrabberConfig

import javax.swing.*
import javax.swing.event.DocumentEvent
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener

import static com.intellij.util.text.DateFormatUtil.getDateFormat
import static java.awt.GridBagConstraints.HORIZONTAL
import static util.DateTimeUtil.floorToDay

@SuppressWarnings("GrUnresolvedAccess")
class Dialog {
	static showDialog(HistoryGrabberConfig grabberConfig, String dialogTitle, Project project,
	                  Closure onApplyCallback, Closure onGrabCallback) {
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

		def currentUIConfig = {
			new HistoryGrabberConfig(
					floorToDay(fromDatePicker.date),
					floorToDay(toDatePicker.date),
					filePathTextField.text,
					grabChangeSizeCheckBox.selected,
					grabOnVcsUpdateCheckBox.selected,
					grabberConfig.lastGrabTime
			)
		}

		DialogBuilder builder = new DialogBuilder(project)
		builder.title = dialogTitle
		builder.centerPanel = rootPanel
		builder.dimensionServiceKey = "CodeHistoryMiningDialog"

		def cancelAction = new AbstractAction("Cancel") {
			@Override void actionPerformed(ActionEvent e) {
				builder.dialogWrapper.close(0)
			}
		}
		cancelAction.putValue(Action.MNEMONIC_KEY, 'C'.charAt(0) as int)
		def applyAction = new AbstractAction("Apply") {
			@Override void actionPerformed(ActionEvent e) {
				onApplyCallback(currentUIConfig())
				grabberConfig = currentUIConfig()
				update()
			}
			def update() {
				setEnabled(currentUIConfig() != grabberConfig)
			}
		}
		applyAction.enabled = false
		applyAction.putValue(Action.MNEMONIC_KEY, 'A'.charAt(0) as int)
		def grabAction = new AbstractAction("Grab") {
			@Override void actionPerformed(ActionEvent e) {
				onGrabCallback(currentUIConfig())
				builder.dialogWrapper.close(0)
			}
		}
		grabAction.putValue(DialogWrapper.DEFAULT_ACTION, Boolean.TRUE)
		builder.with {
			if (SystemInfo.isMac) {
				addAction(cancelAction)
				addAction(applyAction)
				addAction(grabAction)
			} else {
				addAction(grabAction)
				addAction(applyAction)
				addAction(cancelAction)
			}
		}

		filePathTextField.textField.document.addDocumentListener(new DocumentAdapter() {
			@Override protected void textChanged(DocumentEvent e) {
				applyAction.update()
			}
		})
		childrenOf(rootPanel).each{
			if (it.respondsTo("addActionListener")) {
				it.addActionListener(new AbstractAction() {
					@Override void actionPerformed(ActionEvent e) {
						applyAction.update()
					}
				})
			}
		}

		ApplicationManager.application.invokeLater{ builder.showModal(true) } as Runnable
	}

	private static Collection<JComponent> childrenOf(JComponent component) {
		(0..<component.componentCount).collectMany{ int i ->
			Component child = component.getComponent(i)
			(child instanceof JComponent) ? [child] + childrenOf(child) : []
		}
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
