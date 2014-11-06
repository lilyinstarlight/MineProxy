package net.fkmclane.mineproxy.protocol.https;

import java.net.URL;
import java.net.Proxy;
import java.net.ProtocolException;
import java.io.*;
import javax.net.ssl.*;
import java.security.Permission;
import java.security.Principal;
import java.util.Map;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

public class FakeHttpsURLConnection extends HttpsURLConnection {
	private boolean connected;
	URL url;

	protected DelegateHttpURLConnection delegate;

	FakeHttpsURLConnection(URL url, Handler handler) throws IOException {
		this(url, null, handler);
	}

	FakeHttpsURLConnection(URL url, Proxy proxy, Handler handler) throws IOException {
		//We modify the url in the call because super must be first
		super(url = new URL("http", url.getHost(), url.getPort(), url.getFile()), null, handler);
		delegate = new DelegateHttpURLConnection(url, null, handler);
		if(System.getProperty("http.proxySet").equals("true"))
			setProxiedClient(url, System.getProperty("http.proxyHost"), Integer.parseInt(System.getProperty("http.proxyPort")));
	}

	protected FakeHttpsURLConnection(URL url) throws IOException {
		this(url, null, null);
	}

	protected void setNewClient(URL url) throws IOException {
		delegate.setNewClient(url, false);
	}

	protected void setNewClient(URL url, boolean useCache) throws IOException {
		delegate.setNewClient(url, useCache);
	}

	protected void setProxiedClient(URL url, String proxyHost, int proxyPort) throws IOException {
		delegate.setProxiedClient(url, proxyHost, proxyPort);
	}

	protected void setProxiedClient(URL url, String proxyHost, int proxyPort, boolean useCache) throws IOException {
		delegate.setProxiedClient(url, proxyHost, proxyPort, useCache);
	}

	public void connect() throws IOException {
		delegate.connect();
	}

	protected boolean isConnected() {
		return connected;
	}

	protected void setConnected(boolean conn) {
		connected = conn;
	}

	public String getCipherSuite() {
		return null;
	}

	public java.security.cert.Certificate [] getLocalCertificates() {
		return null;
	}

	public java.security.cert.Certificate [] getServerCertificates() throws SSLPeerUnverifiedException {
		return null;
	}

	public javax.security.cert.X509Certificate[] getServerCertificateChain() {
		return null;
	}

	public Principal getPeerPrincipal() throws SSLPeerUnverifiedException {
		return null;
	}

	public Principal getLocalPrincipal() {
		return null;
	}

	public synchronized OutputStream getOutputStream() throws IOException {
		return delegate.getOutputStream();
	}

	public synchronized InputStream getInputStream() throws IOException {
		return delegate.getInputStream();
	}

	public InputStream getErrorStream() {
		return delegate.getErrorStream();
	}

	public void disconnect() {
		delegate.disconnect();
	}

	public boolean usingProxy() {
		return delegate.usingProxy();
	}

	public Map<String,List<String>> getHeaderFields() {
		return delegate.getHeaderFields();
	}

	public String getHeaderField(String name) {
		return delegate.getHeaderField(name);
	}

	public String getHeaderField(int n) {
		return delegate.getHeaderField(n);
	}

	public String getHeaderFieldKey(int n) {
		return delegate.getHeaderFieldKey(n);
	}

	public void setRequestProperty(String key, String value) {
		delegate.setRequestProperty(key, value);
	}

	public void addRequestProperty(String key, String value) {
		delegate.addRequestProperty(key, value);
	}

	public int getResponseCode() throws IOException {
		return delegate.getResponseCode();
	}

	public String getRequestProperty(String key) {
		return delegate.getRequestProperty(key);
	}

	public Map<String,List<String>> getRequestProperties() {
		return delegate.getRequestProperties();
	}

	public void setInstanceFollowRedirects(boolean shouldFollow) {
		delegate.setInstanceFollowRedirects(shouldFollow);
	}

	public boolean getInstanceFollowRedirects() {
		return delegate.getInstanceFollowRedirects();
	}

	public void setRequestMethod(String method) throws ProtocolException {
		delegate.setRequestMethod(method);
	}

	public String getRequestMethod() {
		return delegate.getRequestMethod();
	}

	public String getResponseMessage() throws IOException {
		return delegate.getResponseMessage();
	}

	public long getHeaderFieldDate(String name, long Default) {
		return delegate.getHeaderFieldDate(name, Default);
	}

	public Permission getPermission() throws IOException {
		return delegate.getPermission();
	}

	public URL getURL() {
		return delegate.getURL();
	}

	public int getContentLength() {
		return delegate.getContentLength();
	}

	public long getContentLengthLong() {
		return delegate.getContentLengthLong();
	}

	public String getContentType() {
		return delegate.getContentType();
	}

	public String getContentEncoding() {
		return delegate.getContentEncoding();
	}

	public long getExpiration() {
		return delegate.getExpiration();
	}

	public long getDate() {
		return delegate.getDate();
	}

	public long getLastModified() {
		return delegate.getLastModified();
	}

	public int getHeaderFieldInt(String name, int Default) {
		return delegate.getHeaderFieldInt(name, Default);
	}

	public long getHeaderFieldLong(String name, long Default) {
		return delegate.getHeaderFieldLong(name, Default);
	}

	public Object getContent() throws IOException {
		return delegate.getContent();
	}

	public Object getContent(Class[] classes) throws IOException {
		return delegate.getContent(classes);
	}

	public String toString() {
		return delegate.toString();
	}

	public void setDoInput(boolean doinput) {
		delegate.setDoInput(doinput);
	}

	public boolean getDoInput() {
		return delegate.getDoInput();
	}

	public void setDoOutput(boolean dooutput) {
		delegate.setDoOutput(dooutput);
	}

	public boolean getDoOutput() {
		return delegate.getDoOutput();
	}

	public void setAllowUserInteraction(boolean allowuserinteraction) {
		delegate.setAllowUserInteraction(allowuserinteraction);
	}

	public boolean getAllowUserInteraction() {
		return delegate.getAllowUserInteraction();
	}

	public void setUseCaches(boolean usecaches) {
		delegate.setUseCaches(usecaches);
	}

	public boolean getUseCaches() {
		return delegate.getUseCaches();
	}

	public void setIfModifiedSince(long ifmodifiedsince) {
		delegate.setIfModifiedSince(ifmodifiedsince);
	}

	public long getIfModifiedSince() {
		return delegate.getIfModifiedSince();
	}

	public boolean getDefaultUseCaches() {
		return delegate.getDefaultUseCaches();
	}

	public void setDefaultUseCaches(boolean defaultusecaches) {
		delegate.setDefaultUseCaches(defaultusecaches);
	}

	protected void finalize() throws Throwable {
		delegate.finalize();
	}

	public boolean equals(Object obj) {
		return delegate.equals(obj);
	}

	public int hashCode() {
		return delegate.hashCode();
	}

	public void setConnectTimeout(int timeout) {
		delegate.setConnectTimeout(timeout);
	}

	public int getConnectTimeout() {
		return delegate.getConnectTimeout();
	}

	public void setReadTimeout(int timeout) {
		delegate.setReadTimeout(timeout);
	}

	public int getReadTimeout() {
		return delegate.getReadTimeout();
	}

	public void setFixedLengthStreamingMode (int contentLength) {
		delegate.setFixedLengthStreamingMode(contentLength);
	}

	public void setFixedLengthStreamingMode(long contentLength) {
		delegate.setFixedLengthStreamingMode(contentLength);
	}

	public void setChunkedStreamingMode (int chunklen) {
		delegate.setChunkedStreamingMode(chunklen);
	}
}
