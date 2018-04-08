package net.fkmclane.mineproxy;

import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import javax.swing.JOptionPane;

public class ProxyLauncher {
	private static final String launcher_url = "https://s3.amazonaws.com/Minecraft.Download/launcher/Minecraft.jar";
	private static final String launcher_dir = "minecraft";

	public static void main(String[] args) {
		File jar, settings_file, ca_cert_file, ca_key_file;

		if(args.length > 0) {
			// called with a JAR file as an argument
			jar = new File(args[0]);

			String jar_dir = jar.getParent();
			if(jar_dir == null)
				jar_dir = ".";

			// get files relative to JAR file directory
			settings_file = new File(jar_dir + "/server.properties");
			ca_cert_file = new File(jar_dir + "/ca.crt");
			ca_key_file = new File(jar_dir + "/ca.key");

			if(!jar.exists()) {
				alert("Error: File not found");
				System.exit(1);
			}
		}
		else {
			// called directly
			File minecraft_dir = getMinecraftDirectory();

			// get files relative to minecraft directory
			jar = new File(minecraft_dir + "/" + launcher_dir + ".jar");
			settings_file = new File(minecraft_dir + "/auth.properties");
			ca_cert_file = new File(minecraft_dir + "/ca.crt");
			ca_key_file = new File(minecraft_dir + "/ca.key");

			// download launcher if necessary
			if(!jar.exists()) {
				try {
					System.err.println("MineProxy: Downloading launcher");

					downloadLauncher(launcher_url, jar);
				}
				catch(IOException e) {
					alert("Error downloading launcher: " + e);
					System.exit(1);
				}
			}
		}

		// create settings file if necessary
		if(!settings_file.exists()) {
			try {
				System.err.println("MineProxy: Creating settings file");

				settings_file.createNewFile();
			}
			catch(IOException e) {
				alert("Error creating settings file: " + e);
			}
		}

		// get auth server from settings file
		String auth_server = "";
		try {
			System.err.println("MineProxy: Loading settings file");

			Properties settings = new Properties();
			settings.load(new FileInputStream(settings_file));
			auth_server = settings.getProperty("auth");
		}
		catch(IOException e) {
			alert("Error reading settings file: " + e);
		}

		// create a new proxy if auth server set
		MineProxy proxy = null;
		if(auth_server != null && auth_server.length() > 0) {
			try {
				System.err.println("MineProxy: Creating proxy server");

				// start proxy
				proxy = new MineProxy(ca_cert_file, ca_key_file, auth_server);
				proxy.start();

				// add generated CA certificate to store
				try {
					System.err.println("MineProxy: Checking for CA certificate");

					checkCACertificate(ca_cert_file);
				}
				catch(IOException e) {
					alert("Error adding certificate: " + e);
				}
			}
			catch(Exception e) {
				alert("Error starting proxy server: " + e);
				System.exit(1);
			}
		}

		// prepare jar arguments
		String[] jarargs;
		if (proxy != null) {
			if(args.length > 1) {
				// shift arguments over one
				jarargs = new String[args.length + 2 - 1];
				for(int i = 0; i < args.length - 1; i++)
					jarargs[i + 2] = args[i + 1];
			}
			else {
				jarargs = new String[2];
			}

			// set proxy arguments
			jarargs[0] = "--proxyHost=localhost";
			jarargs[1] = "--proxyPort=" + Integer.toString(proxy.getPort());
		}
		else {
			if(args.length > 1) {
				// shift arguments over one
				jarargs = new String[args.length - 1];
				for(int i = 0; i < jarargs.length; i++)
					jarargs[i] = args[i + 1];
			}
			else {
				jarargs = new String[0];
			}
		}

		// run JAR
		try {
			System.err.println("MineProxy: Loading JAR");

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

		// get directory based on operating system
		if(os.contains("win")) {
			String applicationData = System.getenv("APPDATA");
			dir = new File(applicationData != null ? applicationData : home, "." + launcher_dir + "\\");
		}
		else if(os.contains("mac")) {
			dir = new File(home, "Library/Application Support/" + launcher_dir + "/");
		}
		else if(os.contains("linux") || os.contains("unix")) {
			dir = new File(home, "." + launcher_dir + "/");
		}
		else {
			dir = new File(home, launcher_dir + "/");
		}

		if (!dir.exists() && !dir.mkdirs())
			alert("Error creating Minecraft directory: " + dir);

		return dir;
	}

	private static void checkCACertificate(File ca_cert_file) throws IOException {
		String os = System.getProperty("os.name").toLowerCase();
		String home = System.getProperty("java.home");

		File keytool;
		File cacerts;
		String[] check;
		String[] add;

		// generate keytool commands based on operating system
		if(os.contains("win")) {
			keytool = new File(home + "\\bin\\keytool.exe");
			cacerts = new File(home + "\\lib\\security\\cacerts");
			check = new String[] { keytool.toString(), "-list", "-keystore", cacerts.toString(), "-storepass", "changeit", "-noprompt" };
			add = new String[] { "runas", "/noprofile", "/user:Administrator", "cmd", "/c", keytool.toString().replace(" ", "\\ ") + " -import -noprompt -trustcacerts -keystore " + cacerts.toString().replace(" ", "\\ ") + " -alias mineproxy -file " + ca_cert_file.toString().replace(" ", "\\ ") + " -storepass changeit" };
		}
		else if(os.contains("mac")) {
			keytool = new File(home + "/bin/keytool");
			cacerts = new File(home + "/lib/security/cacerts");
			check = new String[] { keytool.toString(), "-list", "-keystore", cacerts.toString(), "-storepass", "changeit", "-noprompt" };
			add = new String[] { "osascript", "-e", "do shell script \"'" + keytool.toString() + "' -import -noprompt -trustcacerts -keystore '" + cacerts.toString() + "' -alias mineproxy -file '" + ca_cert_file.toString() + "' -storepass changeit" + "\" with administrator privileges" };
		}
		else if(os.contains("linux")) {
			keytool = new File(home + "/bin/keytool");
			cacerts = new File(home + "/lib/security/cacerts");
			check = new String[] { keytool.toString(), "-list", "-keystore", cacerts.toString(), "-storepass", "changeit", "-noprompt" };
			add = new String[] { "pkexec", "bash", "-c", keytool.toString().replace(" ", "\\ ") + " -import -noprompt -trustcacerts -keystore " + cacerts.toString().replace(" ", "\\ ") + " -alias mineproxy -file " + ca_cert_file.toString().replace(" ", "\\ ") + " -storepass changeit" };
		}
		else {
			keytool = new File(home + "/bin/keytool");
			cacerts = new File(home + "/lib/security/cacerts");
			check = new String[] { keytool.toString(), "-list", "-keystore", cacerts.toString(), "-storepass", "changeit", "-noprompt" };
			add = new String[] { "sudo", "bash", "-c", keytool.toString().replace(" ", "\\ ") + " -import -noprompt -trustcacerts -keystore " + cacerts.toString().replace(" ", "\\ ") + " -alias mineproxy -file " + ca_cert_file.toString().replace(" ", "\\ ") + " -storepass changeit" };
		}

		if (!keytool.exists() || !cacerts.exists()) {
			alert("Error adding certificate with " + keytool + ": " + cacerts);
			return;
		}

		Process process;

		// run checker
		process = Runtime.getRuntime().exec(check);

		boolean found = false;

		// find line starting with "mineproxy,"
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String str;
		while ((str = reader.readLine()) != null) {
			if (str.startsWith("mineproxy,")) {
				found = true;
				break;
			}
		}

		if (!found) {
			// run certificate installer
			process = Runtime.getRuntime().exec(add);
			reader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			System.out.println(reader.readLine());
		}
	}

	private static void downloadLauncher(String launcher, File output) throws IOException {
		// open stream to URL
		BufferedInputStream in = new BufferedInputStream(new URL(launcher).openStream());
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output));

		// write 4096 byte blocks at a time
		byte[] buffer = new byte[4096];
		int count;
		while((count = in.read(buffer)) != -1)
			out.write(buffer, 0, count);

		// close URL and file
		out.close();
		in.close();
	}
}
