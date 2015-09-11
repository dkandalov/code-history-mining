package codehistoryminer.plugin

import liveplugin.pluginrunner.PluginRunner
import org.codehaus.groovy.control.CompilationFailedException

public class GroovyScriptRunner {
	private final GroovyScriptRunnerListener listener;
	private GroovyScriptEngine scriptEngine
	private String mainScriptUrl

	public GroovyScriptRunner(GroovyScriptRunnerListener listener = GroovyScriptRunnerListener.none) {
		this.listener = listener;
	}

	public void loadScript(String scriptFileName, String pathToScriptFolder) {
		try {
			mainScriptUrl = new File(pathToScriptFolder + File.separator + scriptFileName).toURI().toURL().toString()
			String pluginFolderUrl = "file:///" + pathToScriptFolder + "/"; // prefix with "file:///" for GroovyScriptEngine

			List<String> classPath = [pluginFolderUrl]
			ClassLoader classLoader = ClasspathAddition.createClassLoaderWithDependencies(classPath);

			// assume that GroovyScriptEngine is thread-safe
			// (according to this http://groovy.329449.n5.nabble.com/Is-the-GroovyScriptEngine-thread-safe-td331407.html)
			scriptEngine = new GroovyScriptEngine(pluginFolderUrl, classLoader)
			try {
				scriptEngine.loadScriptByName(mainScriptUrl);
			} catch (Exception e) {
				listener.runningError(e);
			}

		} catch (IOException e) {
			listener.loadingError("Error creating scripting engine. " + e.getMessage());
		} catch (CompilationFailedException e) {
			listener.loadingError("Error compiling script. " + e.getMessage());
		} catch (LinkageError e) {
			listener.loadingError("Error linking script. " + e.getMessage());
		} catch (Error e) {
			listener.loadingError(e);
		} catch (Exception e) {
			listener.loadingError(e);
		}
	}

	public runScript(Map<String, ?> binding) {
		try {
			scriptEngine.run(mainScriptUrl, createGroovyBinding(binding));
		} catch (Exception e) {
			listener.runningError(e);
		}
	}

	private static Binding createGroovyBinding(Map<String, ?> binding) {
		Binding result = new Binding();
		for (Map.Entry<String, ?> entry : binding.entrySet()) {
			result.setVariable(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private static class ClasspathAddition {
		public static ClassLoader createClassLoaderWithDependencies(List<String> classPath) {
			GroovyClassLoader classLoader = new GroovyClassLoader(PluginRunner.class.getClassLoader());
			for (String path : classPath) {
				if (path.startsWith("file:/")) {
					URL url = new URL(path);
					classLoader.addURL(url);
					classLoader.addClasspath(url.getFile());
				} else {
					classLoader.addURL(new URL("file:///" + path));
					classLoader.addClasspath(path);
				}
			}
			return classLoader;
		}
	}
}