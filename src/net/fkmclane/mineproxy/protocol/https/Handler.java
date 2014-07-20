package net.fkmclane.mineproxy.protocol.https;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.Proxy;

public class Handler extends sun.net.www.protocol.http.Handler {
	protected String proxy;
	protected int proxyPort;

	public Handler() {
		this(null, -1);
	}

	public Handler(String proxy, int port) {
		this.proxy = proxy;
		this.proxyPort = port;
	}

	protected URLConnection openConnection(URL url) throws IOException {
		return openConnection(url, (Proxy)null);
	}

	protected URLConnection openConnection(URL url, Proxy proxy) throws IOException {
		return new FakeHttpsURLConnection(url, proxy, this);
	}
}
