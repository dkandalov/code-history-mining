package codehistorymining;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import groovy.lang.Binding;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;

public class AppComponent implements ApplicationComponent {
	private static final Logger LOG = Logger.getInstance("CodeHistoryMining");

	@Override public void initComponent() {
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
		binding.setVariable("pluginPath", pluginPath());
		return binding;
	}

	private static String pluginPath() throws URISyntaxException {
		ClassLoader classLoader = AppComponent.class.getClassLoader();
		URL resource = classLoader.getResource("plugin.class");
		if (resource == null) throw new IllegalStateException("Failed to find plugin.class");
		return new File(resource.toURI()).getParent();
	}

	private static Method findMethod(String methodName, Class<?> aClass) {
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
}
