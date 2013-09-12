package tk.fkmclane.mineproxy;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;

public class JarLoader {
	public static void run(File jar, String[] args) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, IOException, NoSuchMethodException {
		run(jar.toURI().toURL(), args);
	}

	public static void run(URL jar, String[] args) throws ClassNotFoundException, IllegalAccessException, InvocationTargetException, IOException, NoSuchMethodException {
		URLClassLoader loader = new URLClassLoader(new URL[] { jar });
		JarURLConnection conn = (JarURLConnection)new URL("jar", "", jar + "!/").openConnection();
		String mainclassname = conn.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);

		Class<?> mainclass = loader.loadClass(mainclassname);
		Method main = mainclass.getMethod("main", new Class[] { args.getClass() });
		main.setAccessible(true);

		main.invoke(mainclass, new Object[] { args });
	}
}
