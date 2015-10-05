package codehistoryminer.plugin;

import groovy.lang.Binding;
import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import org.codehaus.groovy.control.CompilationFailedException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroovyScriptRunner {
    private final Listener listener;
    private GroovyScriptEngine scriptEngine;
    private String mainScriptUrl;

    public GroovyScriptRunner() {
        this(Listener.none);
    }

    public GroovyScriptRunner(Listener listener) {
        this.listener = listener;
    }

    public boolean loadScript(String scriptFileName, String pathToScriptFolder) {
        try {
            mainScriptUrl = new File(pathToScriptFolder + File.separator + scriptFileName).toURI().toURL().toString();
            String pluginFolderUrl = "file:///" + pathToScriptFolder + "/"; // prefix with "file:///" for GroovyScriptEngine

            List<String> classPath = new ArrayList<String>();
            classPath.add(pluginFolderUrl);
            ClassLoader classLoader = createClassLoader(classPath);

            // assume that GroovyScriptEngine is thread-safe
            // (according to this http://groovy.329449.n5.nabble.com/Is-the-GroovyScriptEngine-thread-safe-td331407.html)
            scriptEngine = new GroovyScriptEngine(pluginFolderUrl, classLoader);
            try {
                scriptEngine.loadScriptByName(mainScriptUrl);
                return true;
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
        return false;
    }

    public Class[] loadedClasses() {
        return scriptEngine.getGroovyClassLoader().getLoadedClasses();
    }

    public Object runScript(Map<String, ?> binding) {
        try {
            return scriptEngine.run(mainScriptUrl, createGroovyBinding(binding));
        } catch (Exception e) {
            listener.runningError(e);
            return null;
        }
    }

    private static Binding createGroovyBinding(Map<String, ?> binding) {
        Binding result = new Binding();
        for (Map.Entry<String, ?> entry : binding.entrySet()) {
            result.setVariable(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static ClassLoader createClassLoader(List<String> classPath) throws MalformedURLException {
        GroovyClassLoader classLoader = new GroovyClassLoader(GroovyScriptRunner.class.getClassLoader());
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

    public interface Listener {
        Listener none = new Listener() {
            @Override public void loadingError(String message) {}
            @Override public void loadingError(Throwable e) {}
            @Override public void runningError(Throwable e) {}
        };

        void loadingError(String message);
        void loadingError(Throwable e);
        void runningError(Throwable e);
    }

}
