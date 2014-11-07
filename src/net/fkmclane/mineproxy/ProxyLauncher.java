package net.fkmclane.mineproxy;

import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javax.swing.JOptionPane;

public class ProxyLauncher {
	private static final String launcherUrl = "https://s3.amazonaws.com/Minecraft.Download/launcher/Minecraft.jar";

	public static void main(String[] args) {
		File jar, settings_file;

		if(args.length > 0) {
			jar = new File(args[0]);

			String jar_dir = jar.getParent();
			if(jar_dir == null)
				jar_dir = ".";

			settings_file = new File(jar_dir + "/server.properties");

			if(!jar.exists()) {
				alert("Error: File not found");
				System.exit(1);
			}
		}
		else {
			File minecraftdir = getMinecraftDirectory();

			jar = new File(minecraftdir + "/Minecraft.jar");
			settings_file = new File(minecraftdir + "/auth.properties");

			if(!jar.exists()) {
				try {
					downloadLauncher(jar);
				}
				catch(IOException e) {
					alert("Error downloading launcher: " + e);
					System.exit(1);
				}
			}
		}

		if(!settings_file.exists()) {
			try {
				settings_file.createNewFile();
			}
			catch(IOException e) {
				alert("Error creating settings file: " + e);
			}
		}

		String auth_server = "";
		try {
			Properties settings = new Properties();
			settings.load(new FileInputStream(settings_file));
			auth_server = settings.getProperty("auth");
		}
		catch(IOException e) {
			alert("Error reading settings file: " + e);
		}

		MineProxy proxy = null;
		if(auth_server != null && auth_server.length() > 0) {
			try {
				proxy = new MineProxy(auth_server);
				proxy.start();
			}
			catch(Exception e) {
				alert("Error starting proxy server: " + e);
				System.exit(1);
			}
		}

		String[] jarargs = new String[0];
		if(args.length > 1) {
			jarargs = new String[args.length - 1];
			for(int i = 0; i < jarargs.length; i++)
				jarargs[i] = args[i + 1];
		}

		try {
			JarLoader.run(jar, jarargs);
		}
		catch(Exception e) {
			alert("Error running jar: " + e);
			System.exit(1);
		}
	}

	private static void alert(String message) {
		if(GraphicsEnvironment.isHeadless())
			System.err.println("MineProxy: " + message);
		else
			JOptionPane.showMessageDialog(null, message);
	}

	private static File getMinecraftDirectory() {
		String os = System.getProperty("os.name").toLowerCase();
		String home = System.getProperty("user.home", ".");
		File dir;

		if(os.contains("win")) {
			String applicationData = System.getenv("APPDATA");
			dir = new File(applicationData != null ? applicationData : home, ".minecraft/");
		}
		else if(os.contains("mac")) {
			dir = new File(home, "Library/Application Support/minecraft/");
		}
		else if(os.contains("linux") || os.contains("unix")) {
			dir = new File(home, ".minecraft/");
		}
		else {
			dir = new File(home, "minecraft/");
		}

		if (!dir.exists() && !dir.mkdirs())
			alert("Error creating Minecraft directory: " + dir);

		return dir;
	}

	private static void downloadLauncher(File output) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new URL(launcherUrl).openStream());
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output));

		byte[] buffer = new byte[4096];
		int count;
		while((count = in.read(buffer)) != -1)
			out.write(buffer, 0, count);

		out.close();
		in.close();
	}
}
