package codehistoryminer.plugin.historystorage

import codehistoryminer.core.miner.Data
import codehistoryminer.publicapi.lang.Cancelled
import codehistoryminer.core.lang.JBFileUtil
import codehistoryminer.core.lang.Measure
import codehistoryminer.core.historystorage.EventStorageReader
import codehistoryminer.core.historystorage.EventStorageWriter
import codehistoryminer.core.historystorage.FileChangeConverter
import codehistoryminer.core.miner.filechange.FileChangeMiner
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

	List<Data> readAll(String fileName, Cancelled cancelled) {
		measure.measure("Storage.readAll"){
			def listener = new EventStorageReader.Listener() {
				@Override void failedToReadLine(String line, Exception e) { log?.failedToRead(line) }
			}
			def storage = new EventStorageReader("$basePath/$fileName", FileChangeConverter.create(), listener).init()
			storage.readAllEvents(cancelled)
		}
	}

	@SuppressWarnings("GrMethodMayBeStatic")
	String guessProjectNameFrom(String fileName) {
		fileName.replace(".csv", "").replace("-file-events", "")
	}

	@SuppressWarnings("GrMethodMayBeStatic")
	EventStorageReader eventStorageReader(String filePath) {
		new EventStorageReader(filePath, FileChangeConverter.create()).init()
	}

	@SuppressWarnings("GrMethodMayBeStatic")
	EventStorageWriter eventStorageWriter(String filePath) {
		new EventStorageWriter(
				filePath,
				FileChangeMiner.keyAttributes,
				FileChangeConverter.create(),
				new EventStorageWriter.Listener()
		).init()
	}

	interface Log {
		def failedToRead(def line)
	}
}
