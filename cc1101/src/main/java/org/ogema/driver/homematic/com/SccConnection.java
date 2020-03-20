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

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.wiringpi.GpioUtil;

public class SccConnection extends SerialConnection {
	private final Logger logger = LoggerFactory.getLogger(SccConnection.class);

	private static final String SERIAL_PORT = "org.ogema.driver.homematic.serial.port";
	private static final String SERIAL_PORT_DEFAULT = "/dev/ttyAMA0";

	private static final int SERIAL_BAUDRATE = 38400;

	private GpioController gpio = null;
	private GpioPinDigitalOutput pin = null;

	public SccConnection() {
		super();
	}

	@Override
	public SerialPort build() throws IOException {
		String port = System.getProperty(SERIAL_PORT, SERIAL_PORT_DEFAULT);
		logger.info("Connecting Raspberry Pi HomeMatic Stackable Module at port {}", port);
		try {
	        // Check if privileged access is required on the running system and enable non-
	        // privileged GPIO access if not.
	        if (!GpioUtil.isPrivilegedAccessRequired()) {
	            GpioUtil.enableNonPrivilegedAccess();
	        }
	        else {
	            logger.warn("Privileged access is required on this system to access GPIO pins");
	        }
	        gpio = GpioFactory.getInstance();
			
			// Provision gpio pin #17 as output and set it high
			pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "SCC", PinState.LOW);
			pin.setShutdownOptions(true, PinState.LOW);
			pin.setState(PinState.HIGH);
			
			return SerialPortBuilder.newBuilder(port)
					.setBaudRate(SERIAL_BAUDRATE)
					.setDataBits(DataBits.DATABITS_8)
					.setStopBits(StopBits.STOPBITS_1)
					.setParity(Parity.NONE)
					.build();
			
		} catch (RuntimeException e) {
			throw new IOException(e);
		}
	}

	@Override
	public void close() {
		super.close();
		if (gpio != null) {
			gpio.unprovisionPin(pin);
			gpio.shutdown();
			gpio = null;
		}
	}
}
