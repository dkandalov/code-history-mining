package codemining.plugin.ui.templates

import codemining.core.visualizations.templates.Template

class PluginTemplates {
	static pluginTemplate = template("plugin-template.html")

	private static Template template(String fileName) {
		new Template(Template.readFile(fileName, "/codemining/plugin/ui/templates/"))
	}
}
