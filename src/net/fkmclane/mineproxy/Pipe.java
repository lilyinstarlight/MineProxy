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
		start();
	}

	public void run() {
		try {
			byte[] buffer = new byte[4096];
			int count;
			while((count = in.read(buffer)) != -1) {
				out.write(buffer, 0, count);
				out.flush();
			}
		}
		catch(IOException e) {
			//Most likely a socket close
		}
		finally {
			try {
				in.close();
				out.close();
			}
			catch(IOException e) {
				//Ignore
			}
		}
	}
}
