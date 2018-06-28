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

import org.ogema.driver.homematic.manager.asksin.LocalDevice;

public abstract class LocalConnection {

	final String interfaceId;
	final String parameterString;
	protected LocalDevice localDevice;
//	private final Logger logger = org.slf4j.LoggerFactory.getLogger("homematic-driver");
	boolean hasConnection = false;
	final Object connectionLock;

	public LocalConnection(Object lock, String iface, String parameter) {
		this.connectionLock = lock;
		interfaceId = iface;
		parameterString = parameter;
	}

	public String getInterfaceId() {
		return interfaceId;
	}

	public LocalDevice getLocalDevice() {
		return localDevice;
	}

	public void close() {
		localDevice.close();
	}

	public boolean hasConnection() {
		return hasConnection;
	}
}
