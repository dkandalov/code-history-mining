package codehistorymining;

import com.intellij.openapi.components.ApplicationComponent;
import groovy.lang.Binding;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AppComponent implements ApplicationComponent {

	@Override public void initComponent() {
		try {
			Class<?> aClass = Class.forName("plugin");
			Method method = findMethod("run", aClass);
			if (method == null) {
				// TODO log
				return;
			}

			Binding binding = new Binding();
			binding.setVariable("event", null);
			binding.setVariable("project", null);
			binding.setVariable("isIdeStartup", true);
//			binding.setVariable("pluginPath", new File(this.getClass().getClassLoader().getResource(".").toURI()).getAbsolutePath());
			// TODO use binding

			Constructor<?> constructor = aClass.getDeclaredConstructor(Binding.class);
			method.invoke(constructor.newInstance(binding));

		} catch (ClassNotFoundException e) {
			e.printStackTrace(); // TODO
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
//		} catch (URISyntaxException e) {
//			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
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
