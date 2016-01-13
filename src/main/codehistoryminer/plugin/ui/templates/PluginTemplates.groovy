package codehistoryminer.plugin.ui.templates

import codehistoryminer.core.visualizations.templates.Template

class PluginTemplates {
	static pluginTemplate = template("plugin-template.html")

	private static Template template(String fileName) {
		new Template(Template.readFile(fileName, "/codehistoryminer/plugin/ui/templates/"))
	}
}
