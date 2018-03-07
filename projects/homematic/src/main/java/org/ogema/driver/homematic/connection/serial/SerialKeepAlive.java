/*
 * Copyright 2017-18 ISC Konstanz
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

import org.ogema.driver.homematic.Constants;
import org.ogema.driver.homematic.usbconnection.IUsbConnection;

/**
 * 
 * @author Godwin Burkhardt
 * 
 */
public class SerialKeepAlive implements Runnable {

	private static final byte[] GET_VERSION_KEY = "V".getBytes();

	private IUsbConnection connection;
	private volatile boolean running;
	private volatile String address = null;

	public SerialKeepAlive(IUsbConnection context) {
		this.connection = context;
		running = true;
	}

	public void stop() {
		running = false;
	}

	public void setConnectionAddress(String address) {
		this.address = address;
	}

	@Override
	public void run() {
		while (running) {
			try {
				Thread.sleep(Constants.KEEPALIVETIME);
				connection.sendFrame(GET_VERSION_KEY);
			} catch (InterruptedException e) {
			}
		}
	}

}
