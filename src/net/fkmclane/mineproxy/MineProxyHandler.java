package net.fkmclane.mineproxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

public class MineProxyHandler extends Thread {
	public static final String version = "0.4";

	private static final byte SOCKS_VERSION = 0x05;

	private static final byte SOCKS_ATYP_IPV4 = 0x01;
	private static final byte SOCKS_ATYP_IPV6 = 0x04;
	private static final byte SOCKS_ATYP_DOMAIN = 0x03;

	private static final byte SOCKS_COMMAND_CONNECT = 0x01;
	private static final byte SOCKS_COMMAND_BIND = 0x02;
	private static final byte SOCKS_COMMAND_UDP_ASSOCIATE = 0x03;

	private static final byte SOCKS_METHOD_NO_AUTHENTICATION_REQUIRED = 0x00;
	private static final byte SOCKS_METHOD_GSSAPI = 0x01;
	private static final byte SOCKS_METHOD_BASIC_AUTHENTICATION = 0x02;
	private static final byte SOCKS_METHOD_NO_ACCEPTABLE_METHODS = 0x03;

	private static final byte SOCKS_REPLY_SUCCEEDED = 0x00;
	private static final byte SOCKS_REPLY_GENERAL_FAILURE = 0x01;
	private static final byte SOCKS_REPLY_CONNECTION_NOT_ALLOWED = 0x02;
	private static final byte SOCKS_REPLY_NETWORK_UNREACHABLE = 0x03;
	private static final byte SOCKS_REPLY_HOST_UNREACHABLE = 0x04;
	private static final byte SOCKS_REPLY_CONNECTION_REFUSED = 0x05;
	private static final byte SOCKS_REPLY_TTL_EXPIRED = 0x06;
	private static final byte SOCKS_REPLY_COMMAND_NOT_SUPPORTED = 0x07;
	private static final byte SOCKS_REPLY_ADDRESS_TYPE_NOT_SUPPORTED = 0x08;

	private static final Charset http_charset = Charset.forName("ISO-8859-1");

	private static final Pattern mojang = Pattern.compile("http[s]?://(?:authserver\\.mojang\\.com|api\\.mojang\\.com|sessionserver\\.mojang\\.com|textures\\.minecraft\\.net)(.*)");

	private Socket client, remote;
	private CertGen gen;
	private String auth_server;

	public MineProxyHandler(Socket client, CertGen gen, String auth_server) {
		this.client = client;
		this.gen = gen;
		this.auth_server = auth_server;
	}

	public void run() {
		try {
			// get the streams
			InputStream client_in = client.getInputStream();
			OutputStream client_out = client.getOutputStream();

			byte ver = 0, num = 0, cmd = 0, rsv = 0, atyp = 0;

			// handshake
			ver = (byte)client_in.read();
			num = (byte)client_in.read();

			// bail on wrong version
			if (ver != SOCKS_VERSION) {
				client_out.write(SOCKS_VERSION);
				client_out.write(SOCKS_REPLY_GENERAL_FAILURE);
				client.close();
				return;
			}

			byte[] methods = new byte[num];
			for (int i = 0; i < num; i++)
				methods[i] = (byte)client_in.read();

			client_out.write(SOCKS_VERSION);
			client_out.write(SOCKS_METHOD_NO_AUTHENTICATION_REQUIRED);

			// connect
			ver = (byte)client_in.read();
			cmd = (byte)client_in.read();
			rsv = (byte)client_in.read();
			atyp = (byte)client_in.read();

			// bail on wrong version
			if (ver != SOCKS_VERSION) {
				client_out.write(SOCKS_VERSION);
				client_out.write(SOCKS_REPLY_GENERAL_FAILURE);
				client.close();
				return;
			}

			// get address based on address type
			byte[] addr;
			String sock_addr;
			StringBuilder builder = new StringBuilder();
			switch (atyp) {
				case SOCKS_ATYP_IPV4:
					addr = new byte[4];

					addr[0] = (byte)client_in.read();

					builder.append(String.format("%d", addr[0] & 0xff));

					for (int i = 1; i < 4; i++) {
						addr[i] = (byte)client_in.read();

						builder.append('.');
						builder.append(String.format("%d", addr[i] & 0xff));
					}

					sock_addr = builder.toString();
					break;

				case SOCKS_ATYP_IPV6:
					addr = new byte[16];

					addr[0] = (byte)client_in.read();
					addr[1] = (byte)client_in.read();

					builder.append(String.format("%02x", addr[0]));
					builder.append(String.format("%02x", addr[1]));

					for (int i = 1; i < 4; i++) {
						addr[i*2] = (byte)client_in.read();
						addr[i*2 + 1] = (byte)client_in.read();

						builder.append(':');
						builder.append(String.format("%02x", addr[i*2]));
						builder.append(String.format("%02x", addr[i*2 + 1]));
					}

					sock_addr = builder.toString();
					break;

				case SOCKS_ATYP_DOMAIN:
					byte size = (byte)client_in.read();
					addr = new byte[size];
					client_in.read(addr);

					sock_addr = new String(addr);
					break;

				default:
					client_out.write(SOCKS_VERSION);
					client_out.write(SOCKS_REPLY_ADDRESS_TYPE_NOT_SUPPORTED);
					client.close();
					return;
			}

			// get port
			byte[] port = new byte[2];
			client_in.read(port);
			int sock_port = ((port[0] & 0xff) << 8) | (port[1] & 0xff);

			// follow given command
			switch (cmd) {
				case SOCKS_COMMAND_CONNECT:
					client_out.write(SOCKS_VERSION);
					client_out.write(SOCKS_REPLY_SUCCEEDED);
					client_out.write(rsv);
					client_out.write(atyp);
					client_out.write(addr);
					client_out.write(port);
					break;

				case SOCKS_COMMAND_BIND:
					client_out.write(SOCKS_VERSION);
					client_out.write(SOCKS_REPLY_SUCCEEDED);
					client_out.write(rsv);
					client_out.write(atyp);
					client_out.write(addr);
					client_out.write(port);
					client.close();
					return;

				case SOCKS_COMMAND_UDP_ASSOCIATE:
					client_out.write(SOCKS_VERSION);
					client_out.write(SOCKS_REPLY_SUCCEEDED);
					client_out.write(rsv);
					client_out.write(atyp);
					client_out.write(addr);
					client_out.write(port);
					client.close();
					return;
			}

			// connect to endpoint
			System.err.println("MineProxy: Proxying " + sock_addr + ":" + Integer.toString(sock_port));
			if (sock_port != 80 && sock_port != 443) {
				remote = new Socket(sock_addr, sock_port);

				// pipe the two together
				Pipe pipe_client = new Pipe(new BufferedInputStream(client_in), new BufferedOutputStream(remote.getOutputStream()));
				Pipe pipe_remote = new Pipe(new BufferedInputStream(remote.getInputStream()), new BufferedOutputStream(client_out));
				pipe_client.start();
				pipe_remote.start();

				return;
			}

			if (sock_port == 443) {
				// generate certificate
				SSLContext context = gen.generateSSLContext(new String[] { "*.*", "*.*.*" });

				SSLSocket client_ssl = (SSLSocket)context.getSocketFactory().createSocket(client, sock_addr, sock_port, true);
				client_ssl.setUseClientMode(false);

				client = client_ssl;
				client_in = client.getInputStream();
				client_out = client.getOutputStream();
			}

			// just create buffered streams
			client_in = new BufferedInputStream(client_in);
			client_out = new BufferedOutputStream(client_out);

			// intercept and proxy elsewhere
			String[] request = getRequestLine(client_in);
			Map<String, String> headers = extractHeaders(client_in);

			URL url = parseURL((sock_port == 80 ? "http://" : "https://") + headers.get("Host") + request[1], auth_server);
			System.err.println("MineProxy: Rewrote " + (sock_port == 80 ? "http://" : "https://") + headers.get("Host") + request[1] + " to " + url.toString());

			headers.put("Host", url.getHost());
			request[1] = url.getFile();
			if(request[1].length() == 0)
				request[1] = "/";

			remote = new Socket(url.getHost(), url.getPort() == -1 ? url.getDefaultPort() : url.getPort());

			if (url.getProtocol().equals("https")) {
				SSLContext context = gen.generatePlainContext();

				remote = context.getSocketFactory().createSocket(remote, url.getHost(), url.getPort() == -1 ? url.getDefaultPort() : url.getPort(), true);
			}

			InputStream remote_in = new BufferedInputStream(remote.getInputStream());
			OutputStream remote_out = new BufferedOutputStream(remote.getOutputStream());

			// send a whole new request (mostly the same as the incoming)
			sendHTTP(remote_out, request, headers);

			Pipe pipe_client = new Pipe(client_in, remote_out);
			Pipe pipe_remote = new Pipe(remote_in, client_out);
			pipe_client.start();
			pipe_remote.start();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.err.println("MineProxy: Exception caught while proxying request: " + e);
			// do not leave the client and server hanging
			try {
				client.close();
				remote.close();
			}
			catch(Exception ex) {}
		}
	}

	private String readLine(InputStream in) throws IOException {
		CharsetDecoder decoder = http_charset.newDecoder();
		StringBuilder builder = new StringBuilder();

		// byteBuffer and CharBuffer for decoding
		ByteBuffer in_buf = ByteBuffer.allocate(1);
		CharBuffer out_buf = CharBuffer.allocate(1);
		int b;
		while((b = in.read()) != -1) {
			// put the byte in the ByteBuffer
			in_buf.put(0, (byte)b);

			// rewind buffers
			in_buf.rewind();
			out_buf.rewind();

			// decode the byte
			decoder.decode(in_buf, out_buf, true);

			// get the character
			char c = out_buf.get(0);

			// ignore \r and break on \n
			if(c == '\r')
				continue;
			if(c == '\n')
				break;

			builder.append(c);
		}

		return builder.toString();
	}

	private String[] getRequestLine(InputStream in) throws IOException, ProtocolException {
		// split up "METHOD RESOURCE VERSION"
		String[] request_line = readLine(in).split(" ", 3);
		if(request_line.length != 3)
			throw new ProtocolException("Malformed HTTP request line");
		return request_line;
	}

	private Map<String, String> extractHeaders(InputStream in) throws IOException, ProtocolException {
		Map<String, String> headers = new HashMap<String, String>();

		String header;
		while((header = readLine(in)).length() != 0) {
			// find colon
			int delimeter = header.indexOf(":");

			if(delimeter == -1)
				throw new ProtocolException("Malformed HTTP header");

			// trim either side
			String key = header.substring(0, delimeter).trim();
			String val = header.substring(delimeter + 1).trim();

			if(key.isEmpty() || val.isEmpty())
				throw new ProtocolException("Malformed HTTP header");

			// store
			headers.put(key, val);
		}

		return headers;
	}

	private URL parseURL(String url, String auth_server) throws MalformedURLException {
		Matcher mojang_matcher = mojang.matcher(url);
		if(mojang_matcher.matches())
			return new URL(auth_server + mojang_matcher.group(1));

		return new URL(url);
	}

	private void writeLine(OutputStream out, String line) throws IOException {
		CharsetEncoder encoder = http_charset.newEncoder();

		// write an encoded line ending in \r\n
		out.write(encoder.encode(CharBuffer.wrap(line + "\r\n")).array());
	}

	private void sendHTTP(OutputStream out, String[] request, Map<String, String> headers) throws IOException {
		// write each element in the request line
		writeLine(out, request[0] + " " + request[1] + " " + request[2]);

		// and write all of the headers
		for(String header : headers.keySet())
			writeLine(out, header + ": " + headers.get(header));

		// end HTTP request
		writeLine(out, "");

		// make sure the writes get there
		out.flush();
	}
}
