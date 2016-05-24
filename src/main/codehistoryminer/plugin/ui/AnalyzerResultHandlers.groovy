package codehistoryminer.plugin.ui

import codehistoryminer.core.historystorage.TypeConverter
import codehistoryminer.core.historystorage.implementation.CSVConverter
import codehistoryminer.core.lang.JBFileUtil
import codehistoryminer.publicapi.analysis.values.Table

import static codehistoryminer.core.lang.JBFileUtil.findSequentNonexistentFile

class AnalyzerResultHandlers {
	static File saveDataCollectionAsCsvFile(Collection events, String projectName) {
		def converter = new CSVConverter(TypeConverter.Default.create(TimeZone.default))
		def csv = events.first().keySet().join(",") + "\n"
		csv += events.collect { converter.toCsv(it) }.join("\n")

		def projectTempDir = projectTempDir(projectName)
		def file = nextSequentFile(projectTempDir, projectName)
		file.write(csv)
		file
	}

	static Collection<File> saveTablesAsCsvFile(Collection<Table> tables, String projectName) {
		def projectTempDir = projectTempDir(projectName)
		tables.collect{ table ->
			def file = nextSequentFile(projectTempDir, projectName)
			file.write(table.toCsv())
			file.deleteOnExit() // delete so that after IJ counter starts from 0
			file
		}
	}

	private static File nextSequentFile(File projectTempDir, String projectName) {
		def file = findSequentNonexistentFile(projectTempDir, projectName + "-", ".csv")
		if (!file.createNewFile()) throw new IOException("Failed to create file: ${file.absolutePath}")
		file
	}

	private static projectTempDir(String projectName) {
		def projectTempDir = new File(JBFileUtil.tempDirectory, projectName + "-query-results")
		if (!JBFileUtil.createDirectory(projectTempDir)) throw new IOException("Failed to create directory: ${projectTempDir.absolutePath}")
		projectTempDir
	}
}
