package net.fkmclane.mineproxy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;

public class JarLoader {
	public static void run(File jar, String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		run(jar.toURI().toURL(), args);
	}

	public static void run(URL jar, String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		ClassLoader loader = ClassLoader.getSystemClassLoader();

		// add jar to classpath
		Method add_url = URLClassLoader.class.getDeclaredMethod("addURL", new Class[] { URL.class });
		add_url.setAccessible(true);
		add_url.invoke(loader, new Object[] { jar });

		// get main class
		JarURLConnection jarconn = (JarURLConnection)new URL("jar", "", jar + "!/").openConnection();
		Class<?> mainclass = loader.loadClass(jarconn.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS));

		// invoke main method in main class
		Method main = mainclass.getDeclaredMethod("main", new Class[] { String[].class });
		main.setAccessible(true);
		main.invoke(null, new Object[] { args });
	}
}
