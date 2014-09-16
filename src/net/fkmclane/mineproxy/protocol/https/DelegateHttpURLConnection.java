package net.fkmclane.mineproxy.protocol.https;

import java.net.URL;
import java.net.Proxy;
import java.io.*;

import sun.net.www.protocol.http.HttpURLConnection;

public class DelegateHttpURLConnection extends HttpURLConnection {
	protected DelegateHttpURLConnection(URL url, Proxy proxy, Handler handler) {
		super(url, proxy, handler);
	}

	protected void setNewClient(URL url) throws IOException {
		super.setNewClient(url, false);
	}

	protected void setNewClient(URL url, boolean useCache) throws IOException {
		super.setNewClient(url, useCache);
	}

	protected void setProxiedClient(URL url, String proxyHost, int proxyPort) throws IOException {
		super.setProxiedClient(url, proxyHost, proxyPort);
	}

	protected void setProxiedClient(URL url, String proxyHost, int proxyPort, boolean useCache) throws IOException {
		super.setProxiedClient(url, proxyHost, proxyPort, useCache);
	}

	protected void finalize() throws Throwable {
		super.finalize();
	}
}
