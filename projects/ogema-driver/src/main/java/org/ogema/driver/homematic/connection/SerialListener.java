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

public class SerialListener extends Thread {
    private static final long SLEEP_TIME = 100L;

	private final ConnectionListener listener;
    private final DataInputStream input;

	private volatile boolean closed = true;

	public SerialListener(SerialConnection connection) throws IOException {
		this.listener = connection;
		this.input = new DataInputStream(connection.serial.getInputStream());
		
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
		while (!closed) {
			int numBytesInStream;
			byte[] bytesInStream = null;
			try {
				numBytesInStream = input.available();
				if (numBytesInStream > 0) {
					bytesInStream = new byte[numBytesInStream];
					input.read(bytesInStream);
					
					listener.onReceivedFrame(bytesInStream);
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
