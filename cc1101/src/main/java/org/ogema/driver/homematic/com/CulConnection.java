/*
 * Copyright 2016-19 ISC Konstanz
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
package org.ogema.driver.homematic.com;

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

	private static final String SERIAL_PORT = "org.ogema.driver.homematic.serial.port";
	private static final String SERIAL_PORT_DEFAULT = "/dev/ttyUSB0";

	private static final int SERIAL_BAUDRATE = 9600;

	public CulConnection() {
		super();
	}

	@Override
	protected SerialPort build() throws IOException {
		String port = System.getProperty(SERIAL_PORT, SERIAL_PORT_DEFAULT);
		logger.info("Connecting HomeMatic CUL Stick at port {}", port);
		
		return SerialPortBuilder.newBuilder(port)
				.setBaudRate(SERIAL_BAUDRATE)
				.setDataBits(DataBits.DATABITS_7)
				.setStopBits(StopBits.STOPBITS_1)
				.setParity(Parity.EVEN)
				.build();
	}
}
