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
			InputStream client_in = new BufferedInputStream(client.getInputStream());
			OutputStream client_out = new BufferedOutputStream(client.getOutputStream());

			Reader client_reader = new InputStreamReader(client_in);

			String[] request_line = getRequestLine(client_reader);
			Map<String, String> headers = extractHeaders(client_reader);

			URL url = parseURL(new URL(request_line[1]), auth_server);

			headers.put("Host", url.getHost());
			request_line[1] = url.getPath();
			if(request_line[1].length() == 0)
				request_line[1] = "/";

			remote = new Socket(url.getHost(), 80);
			InputStream remote_in = new BufferedInputStream(remote.getInputStream());
			OutputStream remote_out = new BufferedOutputStream(remote.getOutputStream());

			Writer remote_writer = new OutputStreamWriter(remote_out);

			sendRequest(remote_writer, request_line, headers);

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
		while((c = (char)in.read()) != '\n')
			builder.append(c);

		if(builder.charAt(builder.length() - 1) == '\r')
			builder.deleteCharAt(builder.length() - 1);

		return builder.toString();
	}

	private static String[] getRequestLine(Reader in) throws IOException {
		return readLine(in).split(" ");
	}

	private static HashMap<String, String> extractHeaders(Reader in) throws IOException {
		HashMap<String, String> headers = new HashMap<String, String>();

		String header;
		while((header = readLine(in)).length() != 0) {
			int delimeter = header.indexOf(": ");
			if(delimeter == -1)
				break;

			headers.put(header.substring(0, delimeter), header.substring(delimeter + 1));
		}

		return headers;
	}

	private static URL parseURL(URL url, String auth_server) throws MalformedURLException {
		String url_string = url.toString();

		Matcher mojang_matcher = mojang.matcher(url_string);
		if(mojang_matcher.matches()) {
			switch(mojang_matcher.group(1)) {
				case "authenticate":
					return new URL("http://" + auth_server + "/authenticate.php");
				case "refresh":
					return new URL("http://" + auth_server + "/refresh.php");
				case "validate":
					return new URL("http://" + auth_server + "/validate.php");
				case "invalidate":
					return new URL("http://" + auth_server + "/invalidate.php");
				case "signout":
					return new URL("http://" + auth_server + "/signout.php");
				default:
					return url;
			}
		}

		Matcher login_matcher = login.matcher(url_string);
		if(login_matcher.matches())
			return new URL("http://" + auth_server + "/" + login_matcher.group(2));

		Matcher joinserver_matcher = joinserver.matcher(url_string);
		if(joinserver_matcher.matches())
			return new URL("http://" + auth_server + "/joinserver.php" + joinserver_matcher.group(1));

		Matcher checkserver_matcher = checkserver.matcher(url_string);
		if(checkserver_matcher.matches())
			return new URL("http://" + auth_server + "/checkserver.php" + checkserver_matcher.group(1));

		Matcher skin_matcher = skin.matcher(url_string);
		if(skin_matcher.matches())
			return new URL("http://" + auth_server + "/getskin.php?user=" + skin_matcher.group(1));

		Matcher cape_matcher = cape.matcher(url_string);
		if(cape_matcher.matches())
			return new URL("http://" + auth_server + "/getcape.php?user=" + cape_matcher.group(1));

		return url;
	}

	private static void sendRequest(Writer out, String[] request_line, Map<String, String> headers) throws IOException {
		out.write(request_line[0]);
		for(int i = 1; i < request_line.length; i++)
			out.write(" " + request_line[i]);
		out.write("\r\n");

		for(String header : headers.keySet())
			out.write(header + ": " + headers.get(header) + "\r\n");

		out.write("\r\n");

		out.flush();
	}
}
