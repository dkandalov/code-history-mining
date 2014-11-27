package codemining;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import groovy.lang.Binding;
import liveplugin.IDEUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;

import static liveplugin.IDEUtil.askIsUserWantsToRestartIde;
import static liveplugin.IDEUtil.downloadFile;

public class AppComponent implements ApplicationComponent {
	private static final String PLUGIN_LIBS_PATH = PathManager.getPluginsPath() + "/code-history-mining-plugin/lib/";
	private static final Logger LOG = Logger.getInstance("CodeHistoryMining");

	@Override public void initComponent() {
		boolean onClasspath = checkThatGroovyIsOnClasspath();
		if (!onClasspath) return;

		try {

			Class<?> aClass = Class.forName("plugin");
			Method method = findMethod("run", aClass);
			if (method == null) throw new IllegalStateException("Couldn't find 'plugin' class");

			Constructor<?> constructor = aClass.getDeclaredConstructor(Binding.class);
			method.invoke(constructor.newInstance(createBinding()));

		} catch (ClassNotFoundException e) {
			handleException(e);
		} catch (InvocationTargetException e) {
			handleException(e);
		} catch (IllegalAccessException e) {
			handleException(e);
		} catch (InstantiationException e) {
			handleException(e);
		} catch (URISyntaxException e) {
			handleException(e);
		} catch (NoSuchMethodException e) {
			handleException(e);
		} catch (IllegalStateException e) {
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

		NotificationListener listener = new NotificationListener() {
			@Override public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent event) {
                // TODO use later version of groovy
				boolean downloaded = downloadFile("http://repo1.maven.org/maven2/org/codehaus/groovy/groovy-all/2.0.6/", "groovy-all-2.0.6.jar", PLUGIN_LIBS_PATH);
				if (downloaded) {
					notification.expire();
					askIsUserWantsToRestartIde("For Groovy libraries to be loaded IDE restart is required. Restart now?");
				} else {
					NotificationGroup.balloonGroup("CodeHistoryMining")
							.createNotification("Failed to download Groovy libraries", NotificationType.WARNING);
				}
			}
		};
		NotificationGroup.balloonGroup("CodeHistoryMining").createNotification(
				"Code History Mining plugin didn't find Groovy libraries on classpath",
				"Without it plugin won't work. <a href=\"\">Download Groovy libraries</a> (~6Mb)",
				NotificationType.ERROR,
				listener
		).notify(null);

		return false;
	}

	private static boolean isGroovyOnClasspath() {
		return IDEUtil.isOnClasspath("org.codehaus.groovy.runtime.DefaultGroovyMethods");
	}
}
