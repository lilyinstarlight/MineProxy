package net.fkmclane.mineproxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MineProxyHandler extends Thread {
	//Yggdrasil
	public static final Pattern mojang = Pattern.compile("http://authserver\\.mojang\\.com/(.*)");
	//Legacy
	public static final Pattern login = Pattern.compile("http://login\\.minecraft\\.net/(.*)");
	//Common
	public static final Pattern joinserver = Pattern.compile("http://session\\.minecraft\\.net/game/joinserver\\.jsp(.*)");
	public static final Pattern checkserver = Pattern.compile("http://session\\.minecraft\\.net/game/checkserver\\.jsp(.*)");
	public static final Pattern skin = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftSkins/(.+)\\.png");
	public static final Pattern cape = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftCloaks/(.+)\\.png");

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

			//Make a reader only for getting the text headers
			Reader client_reader = new InputStreamReader(client_in);

			//Get the whole HTTP request
			String[] request_line = getRequestLine(client_reader);
			Map<String, String> headers = extractHeaders(client_reader);

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
			remote = new Socket(url.getHost(), url.getPort() == -1 ? 80 : url.getPort());
			InputStream remote_in = new BufferedInputStream(remote.getInputStream());
			OutputStream remote_out = new BufferedOutputStream(remote.getOutputStream());

			//For the CONNECT method, simply connect to the server and tell the client
			if(request_line[0].equals("CONNECT")) {
				//Prepare a writer to notify the client
				Writer client_writer = new OutputStreamWriter(client_out);

				//Prepare response line
				String[] connect_line = { "HTTP/1.1", "200", "Connection Established" };

				//Make a proxy-agent method
				Map<String, String> connect_headers = new HashMap<String, String>();
				connect_headers.put("Proxy-agent", "MineProxy/" + MineProxy.getVersion());

				//Send a connection established response
				sendHTTP(client_writer, connect_line, connect_headers);
			}
			else {
				//Prepare a writer for text headers
				Writer remote_writer = new OutputStreamWriter(remote_out);

				//Send a whole new request (mostly the same as the incoming)
				sendHTTP(remote_writer, request_line, headers);
			}

			//Pipe the data so each can talk to the other
			new Pipe(client_in, remote_out);
			new Pipe(remote_in, client_out);
		}
		catch(Exception e) {
			System.err.println("Exception caught while proxying request: " + e);
			//Don't leave the client and server hanging
			try {
				client.close();
				remote.close();
			}
			catch(IOException ex) {}
		}
	}

	private static String readLine(Reader in) throws IOException {
		StringBuilder builder = new StringBuilder();
		char c;

		//Append characters to a string until (the beginning of) a new line is hit
		while((c = (char)in.read()) != '\r')
			builder.append(c);

		//Skip the \n
		in.skip(1);

		return builder.toString();
	}

	private static String[] getRequestLine(Reader in) throws IOException {
		return readLine(in).split(" ");
	}

	private static HashMap<String, String> extractHeaders(Reader in) throws IOException {
		HashMap<String, String> headers = new HashMap<String, String>();

		String header;
		while((header = readLine(in)).length() != 0) {
			int delimeter = header.indexOf(":");
			if(delimeter == -1)
				break;

			headers.put(header.substring(0, delimeter).trim(), header.substring(delimeter + 1).trim());
		}

		return headers;
	}

	private static URL parseURL(String url, String auth_server) throws MalformedURLException {
		Matcher mojang_matcher = mojang.matcher(url);
		if(mojang_matcher.matches()) {
			switch(mojang_matcher.group(1)) {
				case "authenticate":
					return new URL(auth_server + "/authenticate.php");
				case "refresh":
					return new URL(auth_server + "/refresh.php");
				case "validate":
					return new URL(auth_server + "/validate.php");
				case "invalidate":
					return new URL(auth_server + "/invalidate.php");
				case "signout":
					return new URL(auth_server + "/signout.php");
				default:
					return new URL(url);
			}
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

	private static void sendHTTP(Writer out, String[] line, Map<String, String> headers) throws IOException {
		//Write each element in the request line
		out.write(line[0]);
		for(int i = 1; i < line.length; i++)
			out.write(" " + line[i]);
		out.write("\r\n");

		//And write all of the headers
		for(String header : headers.keySet())
			out.write(header + ": " + headers.get(header) + "\r\n");

		//End HTTP request
		out.write("\r\n");

		//Make sure the writes get there
		out.flush();
	}
}
