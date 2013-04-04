package tk.fostermclane.mineproxy;

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

import mineshafter.proxy.MineProxy;
import net.minecraft.Util;

public class ProxyLauncher {
	public static void main(String[] args) {
		File jar, settingsFile;

		if(args.length > 0) {
			jar = new File(args[0]);
			settingsFile = new File("auth.properties");

			if(!jar.exists()) {
				alert("Error: File not found");
				return;
			}
		}
		else {
			File minecraftdir = Util.getWorkingDirectory();

			jar = new File(minecraftdir + "/minecraft.jar");
			settingsFile = new File(minecraftdir + "/auth.txt");

			if(!jar.exists()) {
				try {
					downloadLauncher(jar);
				}
				catch(IOException e) {
					alert("Error downloading launcher: " + e);
					return;
				}
			}
		}

		if(!settingsFile.exists()) {
			try {
				settingsFile.createNewFile();
			}
			catch(IOException e) {
				alert("Error creating settings file: " + e);
				return;
			}
		}

		String authServer = "";
		try {
			BufferedReader settings = new BufferedReader(new FileReader(settingsFile));
			authServer = settings.readLine();
			settings.close();
		}
		catch(IOException e) {
			alert("Error reading settings file: " + e);
		}

		if(authServer != null && authServer.length() > 0) {
			try {
				startProxy(authServer);
			}
			catch(Exception e) {
				alert("Error starting proxy server: " + e);
				return;
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

	private static void startProxy(String authServer) throws Exception {
		if(authServer.length() == 0)
			return;

		MineProxy proxy = new MineProxy(authServer);
		proxy.start();

		System.setProperty("http.proxyHost", "127.0.0.1");
		System.setProperty("http.proxyPort", Integer.toString(proxy.getPort()));
		System.setProperty("java.net.preferIPv4Stack", "true");
	}

	private static void downloadLauncher(File output) throws IOException {
		BufferedInputStream in = new BufferedInputStream(new URL("https://s3.amazonaws.com/MinecraftDownload/launcher/minecraft.jar").openStream());
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(output), 1024);
		byte buffer[] = new byte[1024];

		while(in.read(buffer, 0, 1024) >= 0)
			out.write(buffer);

		out.close();
		in.close();
	}
}
