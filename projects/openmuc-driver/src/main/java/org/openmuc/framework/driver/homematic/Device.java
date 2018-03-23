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
package org.openmuc.framework.driver.homematic;

import org.ogema.core.channelmanager.driverspi.DeviceLocator;
import org.ogema.driver.homematic.manager.RemoteDevice;

/**
 * 
 * @author baerthbn
 * 
 */
public class Device extends org.ogema.driver.homematic.Device {
	private HomeMaticConnection con;

	public Device(DeviceLocator deviceLocator, HomeMaticConnection connection) {
		super(deviceLocator, null);
		con = connection;
	}

	public RemoteDevice getRemoteDevice() {
		return con.getLocalDevice().getDevices().get(getDeviceAddress());
	}

}
