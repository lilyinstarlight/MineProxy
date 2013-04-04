/*
 * This file is part of Technic Launcher.
 *
 * Copyright (c) 2013-2013, Technic <http://www.technicpack.net/>
 * Technic Launcher is licensed under the Spout License Version 1.
 *
 * Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the Spout License Version 1.
 *
 * Technic Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the Spout License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package mineshafter.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Streams {
	public static int pipeStreams(InputStream in, OutputStream out) throws IOException {
		byte[] b = new byte[8192];
		int read;
		int total = 0;

		while(true) {
			try {
				read = in.read(b);
				if(read == -1) {
					break;
				}
			} catch(IOException e) {
				break;
			}
			out.write(b, 0, read);
			total += read;
		}
		out.flush();
		return total;
	}

	public static void pipeStreamsActive(final InputStream in, final OutputStream out) {
		Thread thread = new Thread("Active Pipe Thread") {

			@Override
			public void run() {
				byte[] b = new byte[8192];
				int count;
				while(true) {
					try {
						count = in.read(b);
						if(count == -1) {
							return;
						}
						out.write(b, 0, count);
						out.flush();
					} catch(IOException e) {
						return;
					}
				}
			}
		};
		thread.start();
	}
}
