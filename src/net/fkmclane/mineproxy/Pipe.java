package net.fkmclane.mineproxy;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

public class Pipe extends Thread {
	InputStream in;
	OutputStream out;

	public Pipe(InputStream in, OutputStream out) {
		this.in = in;
		this.out = out;
	}

	public void run() {
		try {
			// write 4096 byte blocks at a time
			byte[] buffer = new byte[4096];
			int count = 0;
			while((count = in.read(buffer)) != -1) {
				out.write(buffer, 0, count);
				out.flush();
			}
		}
		catch(IOException e) {
			// most likely a socket close
		}
		finally {
			try {
				in.close();
				out.close();
			}
			catch(Exception e) {}
		}
	}
}
