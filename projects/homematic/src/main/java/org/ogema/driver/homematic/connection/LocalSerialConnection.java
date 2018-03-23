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

import java.io.IOException;
import java.util.TooManyListenersException;

import org.ogema.driver.homematic.Constants;
import org.ogema.driver.homematic.connection.serial.SerialConnection;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;

public class LocalSerialConnection  extends LocalConnection {
	
	private final SerialConnection serialConnection;

	public LocalSerialConnection(Object lock, String iface, String parameter) {
		super(lock, iface, parameter);
		
		serialConnection = new SerialConnection(ProtocolType.ASKSIN);
		startConnectThread();
	}
	
	private void startConnectThread() {
		Thread connectSerial = new Thread() {
			@Override
			public void run() {
				while (!hasConnection) {
					try {
						serialConnection.open();
//						serialConnection.setAskSinMode(true);
						synchronized (connectionLock) {
							hasConnection = true;
							localDevice = new LocalDevice(parameterString, serialConnection,ProtocolType.ASKSIN);
							while (!localDevice.getIsReady()) {
								try {
									Thread.sleep(Constants.CONNECT_WAIT_TIME/10);
								} catch (InterruptedException ie) {
									ie.printStackTrace();
								}
							}
							connectionLock.notify();
						}
					} catch (IOException | TooManyListenersException e) {
						try {
							Thread.sleep(Constants.CONNECT_WAIT_TIME);
						} catch (InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}
			}
		};
		connectSerial.setName("homematic-ll-connectSerial");
		connectSerial.start();
	}
}
