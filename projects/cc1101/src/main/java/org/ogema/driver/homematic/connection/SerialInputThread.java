/*
 * Copyright 2016-18 ISC Konstanz
 *
 * This file is part of OpenHomeMatic.
 * For more information visit https://github.com/isc-konstanz/OpenHomeMatic.
 *
 * OpenHomeMatic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenHomeMatic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenHomeMatic.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.ogema.driver.homematic.connection;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerialInputThread extends Thread {
    private static final long SLEEP_TIME = 100L;

	private final Logger logger = LoggerFactory.getLogger(SerialInputThread.class);

	private final ConnectionListener listener;
    private final DataInputStream input;

	private boolean closed = true;
	private boolean once = true;

	public SerialInputThread(ConnectionListener connection, InputStream inputStream) throws IOException {
		this.listener = connection;
		this.input = new DataInputStream(inputStream);
		
		this.setName("OGEMA-HomeMatic-CC1101-listener");
	}

	public void start() {
		this.closed = false;
		super.start();
	}

	public void close() throws IOException {
		this.closed = true;
		this.input.close();
		this.interrupt();
	}

	@Override
	public void run() {
		byte[] bytesOut = null;
		while (!closed) {
			int numBytesInStream;
			byte[] bytesInStream = null;
			try {
				if (once) {
					logger.debug("");
					logger.debug("Listener ready");
					once = false;
				}
				numBytesInStream = input.available();
				if (numBytesInStream > 0) {
					logger.debug("");
					logger.debug("Data available " + numBytesInStream);
					bytesInStream = new byte[numBytesInStream];
					input.read(bytesInStream);
					if (bytesOut == null) {
						bytesOut = bytesInStream;
					}
					else {
						byte[] temp = new byte[bytesOut.length+numBytesInStream];
						System.arraycopy(bytesOut, 0, temp, 0, bytesOut.length);
						System.arraycopy(bytesInStream, 0, temp, bytesOut.length, numBytesInStream);
						bytesOut = temp;
						logger.debug("Data available bytesOut " + bytesOut.length);
					}
					if (bytesInStream[numBytesInStream-2] == 13 && bytesInStream[numBytesInStream-1] == 10) {
						listener.onReceivedFrame(bytesOut);
						bytesOut = null;
					}
					else {
					}
				}
				Thread.sleep(SLEEP_TIME);
				
			} catch (InterruptedException e) {
			} catch (Exception e) {
				listener.onDisconnect();
				return;
			}
		}
	}
}
