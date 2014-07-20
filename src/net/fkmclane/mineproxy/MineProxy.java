package net.fkmclane.mineproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MineProxy extends Thread {
	private static final String version = "0.3";

	private String auth_server;
	private ServerSocket server;
	private int port;

	public MineProxy(String auth_server) throws IOException {
		this.auth_server = auth_server;
		server = new ServerSocket(0);
		port = server.getLocalPort();
		setDaemon(true);
	}

	public void run() {
		String port_string = Integer.toString(port);

		System.setProperty("http.proxyHost", "127.0.0.1");
		System.setProperty("http.proxyPort", port_string);
		System.setProperty("http.proxySet", "true");
		System.setProperty("https.proxyHost", "127.0.0.1");
		System.setProperty("https.proxyPort", port_string);
		System.setProperty("https.proxySet", "true");

		System.setProperty("java.protocol.handler.pkgs", "net.fkmclane.mineproxy.protocol");

		while(true) {
			try {
				Socket connection = server.accept();
				new MineProxyHandler(connection, auth_server);
			}
			catch(Exception e) {
				//Ignore
			}
		}
	}

	public static String getVersion() {
		return version;
	}
}
