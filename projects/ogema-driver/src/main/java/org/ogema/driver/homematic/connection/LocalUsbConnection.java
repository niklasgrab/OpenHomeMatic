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

import org.ogema.driver.homematic.Constants;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;
import org.ogema.driver.homematic.usbconnection.UsbConnection;

public class LocalUsbConnection extends LocalConnection {

	public LocalUsbConnection(Object lock, String iface, String parameter) {
		super(lock, iface, parameter);

		final UsbConnection connection = new UsbConnection();
		
		Thread connectUsb = new Thread() {
			@Override
			public void run() {
				while (!hasConnection) {
					if (connection.connect()) {
						synchronized (connectionLock) {
							hasConnection = true;
							localDevice = new LocalDevice(parameterString, connection, ProtocolType.BYTE);
							connectionLock.notify();
						}
					}
					try {
						Thread.sleep(Constants.CONNECT_WAIT_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		connectUsb.setName("OGEMA-HomeMatic-CC1101-USB-connect");
		connectUsb.start();
	}
}
