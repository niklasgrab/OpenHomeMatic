/*
 * Copyright 2016-17 ISC Konstanz
 *
 * This file is part of OpenSkeleton.
 * For more information visit https://github.com/isc-konstanz/OpenSkeleton.
 *
 * OpenSkeleton is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenSkeleton is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenSkeleton.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.ogema.driver.homematic.connection;

import org.ogema.driver.homematic.Constants;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;
import org.ogema.driver.homematic.usbconnection.UsbConnection;

public class LocalUsbConnection extends LocalConnection {

	public LocalUsbConnection(Object lock, String iface, String parameter) {
		super(lock, iface, parameter);

		final UsbConnection usbConnection = new UsbConnection();
		

		Thread connectUsb = new Thread() {
			@Override
			public void run() {
				while (!hasConnection) {
					if (usbConnection.connect()) {
						synchronized (connectionLock) {
							hasConnection = true;
							localDevice = new LocalDevice(parameterString, usbConnection, ProtocolType.OTHER);
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
		connectUsb.setName("homematic-ll-connectUSB");
		connectUsb.start();
	}
}
