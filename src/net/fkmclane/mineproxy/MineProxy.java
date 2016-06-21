package net.fkmclane.mineproxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class MineProxy extends Thread {
	private String auth_server;
	private ServerSocket server;
	private int port;

	public MineProxy(String auth_server) throws IOException {
		this.auth_server = auth_server;
		server = new ServerSocket(0, 20, InetAddress.getLoopbackAddress());
		port = server.getLocalPort();
		setDaemon(true);
	}

	public void run() {
		String port_string = Integer.toString(port);

		System.setProperty("http.proxyHost", "127.0.0.1");
		System.setProperty("http.proxyPort", port_string);
		System.setProperty("http.proxySet", "true");

		System.setProperty("java.protocol.handler.pkgs", "net.fkmclane.mineproxy.protocol");

		System.err.println("MineProxy: Started proxy on http://localhost:" + port_string);

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
}
