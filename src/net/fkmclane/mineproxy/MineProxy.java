package net.fkmclane.mineproxy;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;

import org.bouncycastle.operator.OperatorException;

public class MineProxy extends Thread {
	private String auth_server;

	private ServerSocket server;
	private int port;

	private CertGen gen;

	public MineProxy(File ca_cert, File ca_key, String auth_server) throws IOException, GeneralSecurityException, OperatorException {
		this.auth_server = auth_server;

		// bind server
		server = new ServerSocket(0, 20, InetAddress.getLoopbackAddress());
		port = server.getLocalPort();
		setDaemon(true);

		// create a certificate generator
		gen = new CertGen(ca_cert, ca_key);
	}

	public int getPort() {
		return port;
	}

	public void run() {
		System.err.println("MineProxy: Started proxy on http://localhost:" + Integer.toString(port));

		while(true) {
			try {
				// handle a connection
				Socket connection = server.accept();
				MineProxyHandler handler = new MineProxyHandler(connection, gen, auth_server);
				handler.start();
			}
			catch(Exception e) {
				// ignore
			}
		}
	}
}
