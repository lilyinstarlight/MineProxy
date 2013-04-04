package net.minecraft;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.util.Map;


public class Util {
	private static File workDir = null;
	public static boolean portable = false;

	public static File getWorkingDirectory() {
		if (workDir == null) {
			if (portable)
				workDir = new File("minecraft.files");
			else
				workDir = getWorkingDirectory("minecraft");
		}

		return workDir;
	}

	public static File getWorkingDirectory(String applicationName) {
		String userHome = System.getProperty("user.home", ".");
		File workingDirectory;

		switch (getPlatform()) {
			case 2:
				workingDirectory = new File(userHome, "." + applicationName + "/");
				break;
			case 0:
				String applicationData = System.getenv("APPDATA");
				if (applicationData != null)
					workingDirectory = new File(applicationData, "." + applicationName + "/");
				else
					workingDirectory = new File(userHome, "." + applicationName + "/");
				break;
			case 1:
				workingDirectory = new File(userHome, "Library/Application Support/" + applicationName);
				break;
			default:
				workingDirectory = new File(userHome, applicationName + "/");
		}

		if ((!workingDirectory.exists()) && (!workingDirectory.mkdirs()))
			throw new RuntimeException("The working directory could not be created: " + workingDirectory);

		return workingDirectory;
	}

	private static int getPlatform() {
		String os = System.getProperty("os.name").toLowerCase();

		if (os.contains("win"))
			return 0;
		if (os.contains("mac"))
			return 1;
		if (os.contains("solaris") || os.contains("sunos") || os.contains("linux") || os.contains("unix"))
			return 2;

		return -1;
	}

	public static String buildQuery(Map<String, Object> paramMap) {
		StringBuilder localStringBuilder = new StringBuilder();

		for (Map.Entry<String, Object> localEntry : paramMap.entrySet()) {
			if (localStringBuilder.length() > 0) {
				localStringBuilder.append('&');
			}

			try {
				localStringBuilder.append(URLEncoder.encode((String)localEntry.getKey(), "UTF-8"));
			} catch (UnsupportedEncodingException localUnsupportedEncodingException1) {
				localUnsupportedEncodingException1.printStackTrace();
			}

			if (localEntry.getValue() != null) {
				localStringBuilder.append('=');
				try {
					localStringBuilder.append(URLEncoder.encode(localEntry.getValue().toString(), "UTF-8"));
				} catch (UnsupportedEncodingException localUnsupportedEncodingException2) {
					localUnsupportedEncodingException2.printStackTrace();
				}
			}
		}

		return localStringBuilder.toString();
	}

	public static String executePost(String targetURL, Map<String, Object> query) {
		String s = buildQuery(query);
		s = executePost(targetURL, s);
		return s;
	}

	public static String executePost(String targetURL, String urlParameters) {
		if (targetURL.startsWith("https://login.minecraft.net"))
			targetURL = "http://login.minecraft.net/";

		URLConnection conn = null;

		try {
			URL url = new URL(targetURL);
			conn = url.openConnection();
			
			conn.setUseCaches(false);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", Integer.toString(urlParameters.getBytes().length));
			conn.setRequestProperty("Content-Language", "en-US");
			conn.setConnectTimeout(10000);

			conn.connect();

			DataOutputStream out = new DataOutputStream(conn.getOutputStream());
			out.writeBytes(urlParameters);
			out.flush();
			out.close();

			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			StringBuilder response = new StringBuilder();
			String line;
			while ((line = in.readLine()) != null) {
				response.append(line);
				response.append('\r');
			}
			in.close();

			return response.toString();
		}
		catch (FileNotFoundException e) {
			System.out.println("executePost: Received 404: " + e);
			return null;
		}
		catch (Exception e) {
			System.out.println("executePost: Error: " + e);
			return null;
		}
	}
}
