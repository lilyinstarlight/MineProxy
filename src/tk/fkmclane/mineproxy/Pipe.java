package tk.fkmclane.mineproxy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

public class Pipe extends Thread {
	BufferedReader in;
	BufferedWriter out;

	public Pipe(BufferedReader in, BufferedWriter out) {
		this.in = in;
		this.out = out;
		start();
	}

	public void run() {
		try {
			char[] buffer = new char[4096];
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
