package net.fkmclane.mineproxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.IOException;
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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MineProxyHandler extends Thread {
	public static final Charset http_charset = Charset.forName("ISO-8859-1");

	//Yggdrasil
	public static final Pattern mojang = Pattern.compile("http(?:s?)://authserver\\.mojang\\.com/(.*)");
	//Legacy
	public static final Pattern login = Pattern.compile("http(?:s?)://login\\.minecraft\\.net/(.*)");
	//Common
	public static final Pattern joinserver = Pattern.compile("http(?:s?)://session\\.minecraft\\.net/game/joinserver\\.jsp(.*)");
	public static final Pattern checkserver = Pattern.compile("http(?:s?)://session\\.minecraft\\.net/game/checkserver\\.jsp(.*)");
	public static final Pattern skin = Pattern.compile("http(?:s?)://skins\\.minecraft\\.net/MinecraftSkins/(.+)\\.png");
	public static final Pattern cape = Pattern.compile("http(?:s?)://skins\\.minecraft\\.net/MinecraftCloaks/(.+)\\.png");

	private Socket client, remote;
	private String auth_server;

	public MineProxyHandler(Socket client, String auth_server) {
		this.client = client;
		this.auth_server = auth_server;
		start();
	}

	public void run() {
		try {
			//Get the streams
			InputStream client_in = new BufferedInputStream(client.getInputStream());
			OutputStream client_out = new BufferedOutputStream(client.getOutputStream());

			//Get the whole HTTP request
			String[] request_line = getRequestLine(client_in);
			Map<String, String> headers = extractHeaders(client_in);

			//Parse the URL and change the destination if necessary
			URL url = parseURL(request_line[1], auth_server);

			//For the CONNECT method (HTTPS tunneling), don't bother with changing around request
			if(!request_line[0].equals("CONNECT")) {
				//Update the Host header and make the new request only have a path, not a full URL
				headers.put("Host", url.getHost());
				request_line[1] = url.getPath();
				if(request_line[1].length() == 0)
					request_line[1] = "/";
			}

			//Open a socket to the new host
			remote = new Socket(url.getHost(), url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
			InputStream remote_in = new BufferedInputStream(remote.getInputStream());
			OutputStream remote_out = new BufferedOutputStream(remote.getOutputStream());

			//For the CONNECT method, simply connect to the server and tell the client
			if(request_line[0].equals("CONNECT")) {
				//Prepare response line
				String[] connect_line = { "HTTP/1.1", "200", "Connection Established" };

				//Make headers and add Proxy-Agent
				Map<String, String> connect_headers = new HashMap<String, String>();
				connect_headers.put("Proxy-Agent", "MineProxy/" + MineProxy.getVersion());

				//Send a Connection Established response
				sendHTTP(client_out, connect_line, connect_headers);
			}
			else {
				//Send a whole new request (mostly the same as the incoming)
				sendHTTP(remote_out, request_line, headers);
			}

			//Pipe the data so each can talk to the other
			new Pipe(client_in, remote_out);
			new Pipe(remote_in, client_out);
		}
		catch(Exception e) {
			System.err.println("Exception caught while proxying request: " + e);
			e.printStackTrace();
			//Don't leave the client and server hanging
			try {
				client.close();
				remote.close();
			}
			catch(Exception ex) {}
		}
	}

	private static String readLine(InputStream in) throws IOException {
		CharsetDecoder decoder = http_charset.newDecoder();
		StringBuilder builder = new StringBuilder();

		//ByteBuffer and CharBuffer for decoding
		ByteBuffer in_buf = ByteBuffer.allocate(1);
		CharBuffer out_buf = CharBuffer.allocate(1);
		int b;
		while((b = in.read()) != -1) {
			//Put the byte in the ByteBuffer
			in_buf.put(0, (byte)b);

			//Rewind buffers
			in_buf.rewind();
			out_buf.rewind();

			//Decode the byte
			decoder.decode(in_buf, out_buf, true);

			//Get the character
			char c = out_buf.get(0);

			//Ignore \r and break on \n
			if(c == '\r')
				continue;
			if(c == '\n')
				break;

			builder.append(c);
		}

		return builder.toString();
	}

	private static String[] getRequestLine(InputStream in) throws IOException, ProtocolException {
		String[] request_line = readLine(in).split(" ", 3);
		if(request_line.length != 3)
			throw new ProtocolException("Malformed HTTP request line");
		return request_line;
	}

	private static HashMap<String, String> extractHeaders(InputStream in) throws IOException, ProtocolException {
		HashMap<String, String> headers = new HashMap<String, String>();

		String header;
		while((header = readLine(in)).length() != 0) {
			int delimeter = header.indexOf(":");

			if(delimeter == -1)
				throw new ProtocolException("Malformed HTTP header");

			String key = header.substring(0, delimeter).trim();
			String val = header.substring(delimeter + 1).trim();

			if(key.isEmpty() || val.isEmpty())
				throw new ProtocolException("Malformed HTTP header");

			headers.put(key, val);
		}

		return headers;
	}

	private static URL parseURL(String url, String auth_server) throws MalformedURLException {
		Matcher mojang_matcher = mojang.matcher(url);
		if(mojang_matcher.matches()) {
			String action = mojang_matcher.group(1);
			if(action.equals("authenticate"))
				return new URL(auth_server + "/authenticate.php");
			else if(action.equals("refresh"))
				return new URL(auth_server + "/refresh.php");
			else if(action.equals("validate"))
				return new URL(auth_server + "/validate.php");
			else if(action.equals("invalidate"))
				return new URL(auth_server + "/invalidate.php");
			else if(action.equals("signout"))
				return new URL(auth_server + "/signout.php");
			else
				return new URL(url);
		}

		Matcher login_matcher = login.matcher(url);
		if(login_matcher.matches())
			return new URL(auth_server + "/" + login_matcher.group(2));

		Matcher joinserver_matcher = joinserver.matcher(url);
		if(joinserver_matcher.matches())
			return new URL(auth_server + "/joinserver.php" + joinserver_matcher.group(1));

		Matcher checkserver_matcher = checkserver.matcher(url);
		if(checkserver_matcher.matches())
			return new URL(auth_server + "/checkserver.php" + checkserver_matcher.group(1));

		Matcher skin_matcher = skin.matcher(url);
		if(skin_matcher.matches())
			return new URL(auth_server + "/getskin.php?user=" + skin_matcher.group(1));

		Matcher cape_matcher = cape.matcher(url);
		if(cape_matcher.matches())
			return new URL(auth_server + "/getcape.php?user=" + cape_matcher.group(1));

		return new URL(url);
	}

	private static void writeLine(OutputStream out, String line) throws IOException {
		CharsetEncoder encoder = http_charset.newEncoder();

		//Write an encoded line ending in \r\n
		out.write(encoder.encode(CharBuffer.wrap(line + "\r\n")).array());
	}

	private static void sendHTTP(OutputStream out, String[] request, Map<String, String> headers) throws IOException {
		//Write each element in the request line
		writeLine(out, request[0] + " " + request[1] + " " + request[2]);

		//And write all of the headers
		for(String header : headers.keySet())
			writeLine(out, header + ": " + headers.get(header));

		//End HTTP request
		writeLine(out, "");

		//Make sure the writes get there
		out.flush();
	}
}
