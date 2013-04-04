package mineshafter.proxy;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.net.BindException;


public class MineProxy extends Thread {
	public static String authServer;
	private int port = -1;

	// Patterns
	public static Pattern SKIN_URL = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftSkins/(.+?)\\.png");
	public static Pattern CAPE_URL = Pattern.compile("http://skins\\.minecraft\\.net/MinecraftCloaks/(.+?)\\.png");
	public static Pattern LOGIN_URL = Pattern.compile("http://login\\.minecraft\\.net/");
	public static Pattern JOINSERVER_URL = Pattern.compile("http://session\\.minecraft\\.net/game/joinserver\\.jsp(.*)");
	public static Pattern CHECKSERVER_URL = Pattern.compile("http://session\\.minecraft\\.net/game/checkserver\\.jsp(.*)");
	public static Pattern AUDIOFIX_URL = Pattern.compile("http://s3\\.amazonaws\\.com/MinecraftResources/");
	public static Pattern DL_BUKKIT = Pattern.compile("http://dl.bukkit.org/(.+?)");

	/* NTS: See if this is still needed */
	public Hashtable<String, byte[]> skinCache;
	public Hashtable<String, byte[]> capeCache;

	public MineProxy(String currentAuthServer) {
		setName("MineProxy Thread");

		MineProxy.authServer = currentAuthServer; // TODO maybe change this leave it for now 

		skinCache = new Hashtable<String, byte[]>();
		capeCache = new Hashtable<String, byte[]>();
	}

	@SuppressWarnings("resource")
	public void run() {
		try {
			ServerSocket server = null;
			int port = 9010; // A lot of other applications use the 80xx range,
			// let's try for some less crowded real-estate
			while (port < 12000) { // That should be enough
				try {
					System.out.println("Trying to proxy on port " + port);
					byte[] loopback = {127, 0, 0, 1};
					server = new ServerSocket(port, 16, InetAddress.getByAddress(loopback));
					this.port = port;
					System.out.println("Proxying successful");
					break;
				}
				catch (BindException ex) {
					port++;
				}
			}

			while(true) {
				Socket connection = server.accept();

				MineProxyHandler handler = new MineProxyHandler(this, connection);
				handler.start();
			}
		}
		catch(IOException ex) {
			System.err.println("Error in server accept loop: " + ex.getLocalizedMessage());
		}
	}


	public int getPort() {
		while (port < 0) {
			try {
				sleep(50);
			}
			catch (InterruptedException ex) {
				System.err.println("Interrupted while waiting for port: " + ex.getLocalizedMessage());
			}
		}

		return port;
	}
}
