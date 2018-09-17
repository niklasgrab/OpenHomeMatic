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
import com.pi4j.io.serial.Baud;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialConfig;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialFactory;

public class SccConnection extends SerialConnection {
	private final Logger logger = LoggerFactory.getLogger(SccConnection.class);

	private GpioController gpio;
	private GpioPinDigitalOutput pin17;
	private GpioPinDigitalOutput pin18;

	private Serial serial;
	private SerialListener listener;

	public SccConnection() {
		super();
		listener = new SerialListener(this);
	}

	@Override
	public void openPort() throws IOException {
		String port = System.getProperty(SERIAL_PORT, Serial.DEFAULT_COM_PORT);
		logger.info("Connecting Raspberry Pi HomeMatic Stackable Module at port {}", port);
		try {
			// Provision gpio pin #17 and #18 as an output pins and turn them on
			gpio = GpioFactory.getInstance();
			pin17 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "SCC17", PinState.HIGH);
//			pin18 = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "SCC18", PinState.HIGH);
			pin17.setShutdownOptions(true, PinState.LOW);
//			pin18.setShutdownOptions(true, PinState.LOW);
			
			// Register the serial data listener
			serial = SerialFactory.createInstance();
			serial.addListener(listener);
			
			SerialConfig config = new SerialConfig().device(port)
					.baud(Baud._38400);
//					.dataBits(DataBits._7)
//					.stopBits(StopBits._1)
//					.parity(Parity.EVEN)
//					.flowControl(FlowControl.NONE);
			
			serial.open(config);
			
		} catch (RuntimeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void write(byte[] data) throws IOException {
		serial.write(data);
		serial.flush();
	}

	@Override
	public void closePort() throws IOException {
		try {
			serial.close();
			
			gpio.unprovisionPin(pin17);
			gpio.unprovisionPin(pin18);
			gpio.shutdown();
			
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
