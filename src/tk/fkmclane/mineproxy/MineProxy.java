package tk.fkmclane.mineproxy;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MineProxy extends Thread {
	private String auth_server;
	private ServerSocket server;
	private int port;

	public MineProxy(String auth_server) throws IOException {
		this.auth_server = auth_server;
		server = new ServerSocket(0);
		port = server.getLocalPort();
	}

	public void run() {
		System.setProperty("http.proxyHost", "127.0.0.1");
		System.setProperty("http.proxyPort", Integer.toString(port));
		System.setProperty("https.proxyHost", "127.0.0.1");
		System.setProperty("https.proxyPort", Integer.toString(port));

		while(true) {
			try {
				Socket connection = server.accept();
				new MineProxyHandler(connection, auth_server);
			}
			catch(IOException e) {
				//Ignore
			}
		}
	}
}
