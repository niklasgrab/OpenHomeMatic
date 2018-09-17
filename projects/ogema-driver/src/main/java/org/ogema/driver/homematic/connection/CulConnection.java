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
import java.io.DataOutputStream;
import java.io.IOException;

import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CulConnection extends SerialConnection {
	private final Logger logger = LoggerFactory.getLogger(CulConnection.class);

	private static final String SERIAL_PORT_DEFAULT = "/dev/ttyUSB0";

	private static final int SERIAL_BAUDRATE = 9600;

	private SerialPort serial;
	private DataInputStream input;
	private DataOutputStream output;
	private Thread listener;

	public CulConnection() {
		super();
	}

	@Override
	protected void openPort() throws IOException {
		String port = System.getProperty(SERIAL_PORT, SERIAL_PORT_DEFAULT);
		logger.info("Connecting HomeMatic CUL Stick at port {}", port);
		
		serial = SerialPortBuilder.newBuilder(port)
				.setBaudRate(SERIAL_BAUDRATE)
				.setDataBits(DataBits.DATABITS_7)
				.setStopBits(StopBits.STOPBITS_1)
				.setParity(Parity.EVEN)
				.build();
		
		input = new DataInputStream(serial.getInputStream());
		output = new DataOutputStream(serial.getOutputStream());
		
		listener = new SerialListener(this);
		listener.setName("OGEMA-HomeMatic-CC1101-CUL-listener");
		listener.start();
	}

	@Override
	public void closePort() throws IOException {
		try {
			listener.interrupt();
			
			input.close();
			output.close();
			serial.close();
			
		} catch (NullPointerException e) {
			throw new IOException(e);
		}
	}

	@Override
	protected void write(byte[] data) throws IOException {
		output.write(data);
		output.flush();
	}

	private class SerialListener extends Thread {

		private final ConnectionListener listener;

		public SerialListener(ConnectionListener listener) {
			this.listener = listener;
		}

		@Override
		public void run() {
			while (!isClosed()) {
				int numBytesInStream;
				byte[] bytesInStream = null;
				try {
					numBytesInStream = input.available();
					if (numBytesInStream > 0) {
						bytesInStream = new byte[numBytesInStream];
						input.read(bytesInStream);

						listener.onReceivedFrame(bytesInStream);
					}
				}
				catch (IOException e) {
					close();
					e.printStackTrace();
					return;
				}
			}
		}
	}

}
