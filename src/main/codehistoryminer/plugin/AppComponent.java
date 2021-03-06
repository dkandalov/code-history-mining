package codehistoryminer.plugin;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import groovy.lang.Binding;
import liveplugin.LivePluginAppComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;

import static liveplugin.IdeUtil.askIfUserWantsToRestartIde;
import static liveplugin.IdeUtil.downloadFile;

public class AppComponent implements ApplicationComponent {
    private static final String pluginId = "CodeHistoryMining";
    private static final String PLUGIN_LIBS_PATH = PathManager.getPluginsPath() + "/code-history-mining-plugin/lib/";
    private static final Logger LOG = Logger.getInstance(pluginId);

    @Override public void initComponent() {
		boolean onClasspath = checkThatGroovyIsOnClasspath();
		if (!onClasspath) return;

		try {

			Class<?> aClass = Class.forName("codehistoryminer.plugin.plugin");
			Method method = findMethod("run", aClass);
			if (method == null) {
				throw new IllegalStateException("Couldn't find 'codehistoryminer.plugin.plugin' class");
			}

			Constructor<?> constructor = aClass.getDeclaredConstructor(Binding.class);
			method.invoke(constructor.newInstance(createBinding()));

		} catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
				 NoSuchMethodException | URISyntaxException | IllegalStateException e) {
			handleException(e);
		}
    }

	private static void handleException(Exception e) {
		LOG.error("Error during initialization", e);
	}

	private static Binding createBinding() throws URISyntaxException {
		Binding binding = new Binding();
		binding.setVariable("event", null);
		binding.setVariable("project", null);
		binding.setVariable("isIdeStartup", true);
		binding.setVariable("pluginPath", PathManager.getJarPathForClass(AppComponent.class));
		return binding;
	}

	@Nullable private static Method findMethod(String methodName, Class<?> aClass) {
		for (Method method : aClass.getDeclaredMethods()) {
			if (method.getName().equals(methodName)) return method;
		}
		return null;
	}

	@Override public void disposeComponent() {
	}

	@NotNull @Override public String getComponentName() {
		return this.getClass().getName();
	}

	private static boolean checkThatGroovyIsOnClasspath() {
		if (isGroovyOnClasspath()) return true;

		NotificationListener listener = (notification, event) -> {
			boolean downloaded = downloadFile("http://repo1.maven.org/maven2/org/codehaus/groovy/groovy-all/2.4.6/", "groovy-all-2.4.6.jar", PLUGIN_LIBS_PATH);
			if (downloaded) {
				notification.expire();
				askIfUserWantsToRestartIde("For Groovy libraries to be loaded IDE restart is required. Restart now?");
			} else {
				NotificationGroup.balloonGroup(pluginId)
						.createNotification("Failed to download Groovy libraries", NotificationType.WARNING);
			}
		};
		NotificationGroup.balloonGroup(pluginId).createNotification(
				"Code History Mining plugin didn't find Groovy libraries on classpath",
				"Without it plugin won't work. <a href=\"\">Download Groovy libraries</a> (~6Mb)",
				NotificationType.ERROR,
				listener
		).notify(null);

		return false;
	}

	private static boolean isGroovyOnClasspath() {
		return isOnClasspath("org.codehaus.groovy.runtime.DefaultGroovyMethods");
	}

	private static boolean isOnClasspath(String className) {
		URL resource = LivePluginAppComponent.class.getClassLoader().getResource(className.replace(".", "/") + ".class");
		return resource != null;
	}
}
