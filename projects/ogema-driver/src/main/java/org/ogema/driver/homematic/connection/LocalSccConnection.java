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
import java.util.TooManyListenersException;

import org.ogema.driver.homematic.Constants;
import org.ogema.driver.homematic.connection.serial.SccConnection;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;

public class LocalSccConnection extends LocalConnection {

	private final SccConnection connection;

	public LocalSccConnection(Object lock, String iface, String parameter) {
		super(lock, iface, parameter);
		
		connection = new SccConnection(ProtocolType.ASKSIN);
		startConnectThread();
	}

	private void startConnectThread() {
		Thread connectScc = new Thread() {
			@Override
			public void run() {
				synchronized (connectionLock) {
					while (!hasConnection) {
						try {
							connection.open();
	//						serialConnection.setAskSinMode(true);
							hasConnection = true;
							localDevice = new LocalDevice(parameterString, connection, ProtocolType.ASKSIN);
							while (!localDevice.getIsReady()) {
								try {
									Thread.sleep(Constants.CONNECT_WAIT_TIME/10);
								} catch (InterruptedException ie) {
									ie.printStackTrace();
								}
							}
							connectionLock.notify();
						
						} catch (IOException | TooManyListenersException e) {
							try {
								Thread.sleep(Constants.CONNECT_WAIT_TIME);
								
							} catch (InterruptedException ie) {
								ie.printStackTrace();
							}
						}
					}
				}
			}
		};
		connectScc.setName("OGEMA-HomeMatic-CC1101-SCC-connect");
		connectScc.start();
	}
}
