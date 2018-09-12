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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPortException;

public class SccConnection extends SerialConnection {
	private final Logger logger = LoggerFactory.getLogger(SccConnection.class);

	private static final int SERIAL_BAUDRATE = 38400;

	private GpioController gpio;
	private GpioPinDigitalOutput pin;
	private Serial port;

	public SccConnection() {
		super();
	}

	@Override
	public void openPort() throws IOException {
		logger.info("Connecting Raspberry Pi HomeMatic Stackable Module");
		try {
			// Provision gpio pin #17 as an output pin and turn on
			gpio = GpioFactory.getInstance();
			pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "SSC", PinState.HIGH);
			pin.setShutdownOptions(true, PinState.LOW);
			
			// Register the serial data listener
			port = SerialFactory.createInstance();
			port.addListener(new SerialListener(this));
			port.open(Serial.DEFAULT_COM_PORT, SERIAL_BAUDRATE);
			
		} catch (RuntimeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(byte[] data) throws IOException {
		if (port == null) throw new SerialPortException("Serial port is not open");
		
		String dataStr = new String(data, "UTF-8");
		
		port.writeln(dataStr);
		port.flush();
	}

	@Override
	public void closePort() throws IOException {
		try {
			pin.low();
			pin.unexport();
			port.close();
			
		} catch (IllegalStateException | NullPointerException e) {
			throw new IOException(e);
		}
	}

	private class SerialListener implements SerialDataEventListener {

		private final ConnectionListener listener;

		public SerialListener(ConnectionListener listener) {
			this.listener = listener;
		}

		@Override
		public void dataReceived(SerialDataEvent event) {
			if (logger.isTraceEnabled()) {
				logger.trace("Notified about received data");
			}
			try {
				String data = event.getAsciiString();
				if (!data.isEmpty()) {
					listener.onReceivedFrame(data.trim().getBytes());
				}
			} catch (IOException e) {
				logger.warn("Failed reading serial event data");
			}
		}
	}

}
