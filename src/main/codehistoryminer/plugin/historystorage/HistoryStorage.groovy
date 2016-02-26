package codehistoryminer.plugin.historystorage

import codehistoryminer.core.common.events.Event
import codehistoryminer.publicapi.lang.Cancelled
import codehistoryminer.core.common.langutil.JBFileUtil
import codehistoryminer.core.common.langutil.Measure
import codehistoryminer.core.historystorage.EventStorageReader
import codehistoryminer.core.historystorage.EventStorageWriter
import codehistoryminer.core.historystorage.FileChangeEventConverter
import codehistoryminer.core.vcs.miner.FileChangeEventMiner
import org.jetbrains.annotations.Nullable

class HistoryStorage {
	private final String basePath
	private final Log log
	private final Measure measure

	HistoryStorage(String basePath = null, Measure measure = null, @Nullable Log log = null) {
		this.basePath = basePath
		this.measure = measure
		this.log = log
	}

	File[] filesWithCodeHistory() {
		new File(basePath).listFiles(new FileFilter() {
			@Override boolean accept(File pathName) { pathName.name.endsWith(".csv") }
		})
	}

	HistoryGrabberConfig loadGrabberConfigFor(String projectName) {
		HistoryGrabberConfig.loadGrabberConfigFor(projectName, basePath) {
			HistoryGrabberConfig.defaultConfig().withOutputFilePath("${basePath}/${projectName + "-file-events.csv"}")
		}
	}

	def saveGrabberConfigFor(String projectName, HistoryGrabberConfig config) {
		HistoryGrabberConfig.saveGrabberConfigOf(projectName, basePath, config)
	}

	boolean isValidNewFileName(String fileName) {
		fileName.length() > 0 && !new File("$basePath/$fileName").exists()
	}

	def rename(String fileName, String newFileName) {
		JBFileUtil.rename(new File("$basePath/$fileName"), new File("$basePath/$newFileName"))
	}

	def delete(String fileName) {
		JBFileUtil.delete(new File("$basePath/$fileName"))
	}

	def historyExistsFor(String fileName) {
		new File("$basePath/$fileName").exists()
	}

	List<Event> readAllEvents(String fileName, Cancelled cancelled) {
		measure.measure("Storage.readAllEvents"){
			def listener = new EventStorageReader.Listener() {
				@Override void failedToReadLine(String line, Exception e) { log?.failedToRead(line) }
			}
			def storage = new EventStorageReader("$basePath/$fileName", FileChangeEventConverter.create(), listener).init()
			storage.readAllEvents(cancelled)
		}
	}

	@SuppressWarnings("GrMethodMayBeStatic")
	String guessProjectNameFrom(String fileName) {
		fileName.replace(".csv", "").replace("-file-events", "")
	}

	@SuppressWarnings("GrMethodMayBeStatic")
	EventStorageReader eventStorageReader(String filePath) {
		new EventStorageReader(filePath, FileChangeEventConverter.create()).init()
	}

	@SuppressWarnings("GrMethodMayBeStatic")
	EventStorageWriter eventStorageWriter(String filePath) {
		new EventStorageWriter(
				filePath,
				FileChangeEventMiner.keyAttributes,
				FileChangeEventConverter.create(),
				new EventStorageWriter.Listener()
		).init()
	}

	interface Log {
		def failedToRead(def line)
	}
}
