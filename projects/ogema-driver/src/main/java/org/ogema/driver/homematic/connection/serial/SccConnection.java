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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialFactory;


public class SccConnection extends SerialConnectionHandler {
	private final Logger logger = LoggerFactory.getLogger(SccConnection.class);

	private GpioController gpio;
	private GpioPinDigitalOutput pin;
	private Serial port;

	public SccConnection() {
		super();
	}

	@Override
	protected AutoCloseable createSerialPort() throws IOException {
		logger.debug("Opening connection to Raspberry Pi serial port");
		
		// Provision gpio pin #17 as an output pin and turn on
		gpio = GpioFactory.getInstance();
		pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "SSC", PinState.HIGH);
		pin.setShutdownOptions(true, PinState.LOW);
		
		// Register the serial data listener
		port = SerialFactory.createInstance();
		port.open(Serial.DEFAULT_COM_PORT, 38400);
		return port;
	}

	@Override
	protected DataInputStream getDataInputStream() throws IOException {
		return new DataInputStream(port.getInputStream());
	}
	
	@Override
	protected DataOutputStream getDataOutputStream() throws IOException {
		return new DataOutputStream(port.getOutputStream());
	}
	
}
