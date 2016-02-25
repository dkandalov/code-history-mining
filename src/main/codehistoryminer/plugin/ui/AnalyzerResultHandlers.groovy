package codehistoryminer.plugin.ui

import codehistoryminer.core.analysis.values.Table
import codehistoryminer.core.historystorage.TypeConverter
import codehistoryminer.core.historystorage.implementation.CSVConverter
import com.intellij.openapi.util.io.FileUtil

class AnalyzerResultHandlers {
	static File saveAsCsvFile(Table result, String fileName) {
		def file = FileUtil.createTempFile(fileName, "")
		file.renameTo(file.absolutePath + ".csv")
		file.write(result.toCsv())
		file
	}

	static File saveAsCsvFile(Collection events, String fileName) {
		def converter = new CSVConverter(TypeConverter.Default.create(TimeZone.default))
		def csv = events.first().keySet().join(",") + "\n"
		csv += events.collect { converter.toCsv(it) }.join("\n")

		def file = FileUtil.createTempFile(fileName, "")
		file.renameTo(file.absolutePath + ".csv")
		file.write(csv)
		file
	}
}
