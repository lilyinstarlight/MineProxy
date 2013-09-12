package tk.fkmclane.mineproxy;

import java.awt.GraphicsEnvironment;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import javax.swing.JOptionPane;

public class ProxyLauncher {
	public static void main(String[] args) {
		File jar, settings_file;

		if(args.length > 0) {
			jar = new File(args[0]);
			settings_file = new File("auth.properties");

			if(!jar.exists()) {
				alert("Error: File not found");
				System.exit(1);
			}
		}
		else {
			File minecraftdir = getMinecraftDirectory();

			jar = new File(minecraftdir + "/Minecraft.jar");
			settings_file = new File(minecraftdir + "/auth.txt");

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
			BufferedReader settings = new BufferedReader(new FileReader(settings_file));
			auth_server = settings.readLine();
			settings.close();
		}
		catch(IOException e) {
			alert("Error reading settings file: " + e);
		}

		if(auth_server != null && auth_server.length() > 0) {
			try {
				startProxy(auth_server);
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
			alert("Error starting jar: " + e);
			System.exit(1);
		}
	}

	private static void alert(String message) {
		if(GraphicsEnvironment.isHeadless())
			System.out.println(message);
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
			dir = new File(home, "Library/Application Support/minecraft");
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

	private static void startProxy(String auth_server) throws Exception {
		if(auth_server.length() == 0)
			return;

		MineProxy proxy = new MineProxy(auth_server);
		proxy.start();
	}

	private static void downloadLauncher(File output) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new URL("https://s3.amazonaws.com/Minecraft.Download/launcher/Minecraft.jar").openStream());
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output), 1024);

		byte[] buffer = new byte[4096];
		int count;
		while((count = in.read(buffer)) != -1)
			out.write(buffer, 0, count);

		out.close();
		in.close();
	}
}
