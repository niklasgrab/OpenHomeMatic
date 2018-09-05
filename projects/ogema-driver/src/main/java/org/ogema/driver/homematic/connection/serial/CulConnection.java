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
package org.ogema.driver.homematic.connection.serial;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.openmuc.jrxtx.DataBits;
import org.openmuc.jrxtx.Parity;
import org.openmuc.jrxtx.SerialPort;
import org.openmuc.jrxtx.SerialPortBuilder;
import org.openmuc.jrxtx.StopBits;

public class CulConnection extends SerialConnectionHandler {
	private static final String SERIAL_PORT_KEY = "org.openmuc.framework.driver.homematic.serial.port";
	private static final String SERIAL_PORT_DEFAULT = "/dev/ttyUSB0";

	private static final int SERIAL_BAUDRATE = 9600;

	private String serialPortName;
	private SerialPort serialPort;

	public CulConnection() {
		super();
	}

	@Override
	protected Closeable createSerialPort() throws IOException {
		serialPortName = System.getProperty(SERIAL_PORT_KEY, SERIAL_PORT_DEFAULT);
        serialPort = SerialPortBuilder.newBuilder(serialPortName)
                .setDataBits(DataBits.DATABITS_7)
                .setStopBits(StopBits.STOPBITS_1)
                .setParity(Parity.EVEN)
                .setBaudRate(SERIAL_BAUDRATE)
                .build();
		return serialPort;
	}

	@Override
	protected DataInputStream getDataInputStream() throws IOException {
		return new DataInputStream(serialPort.getInputStream());
	}
	
	@Override
	protected DataOutputStream getDataOutputStream() throws IOException {
		return new DataOutputStream(serialPort.getOutputStream());
	}
		
	public void setParameters(int baudrate, DataBits dataBits, StopBits stopBits, Parity parity) throws IOException {
		if (serialPort.getBaudRate() != baudrate) {
			serialPort.setBaudRate(baudrate);
		}
		if (!serialPort.getDataBits().equals(dataBits)) {
			serialPort.setDataBits(dataBits);
		}
		if (!serialPort.getStopBits().equals(stopBits)) {
			serialPort.setStopBits(stopBits);
		}
		if (!serialPort.getParity().equals(parity)) {
			serialPort.setParity(parity);
		}
	}

}
