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
package org.ogema.driver.homematic.manager.asksin.devices;

import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.driver.homematic.manager.RemoteDevice;

public class PowerMeter extends org.ogema.driver.homematic.manager.devices.PowerMeter {

	public PowerMeter(RemoteDevice rd) {
		super(rd);
	}

	@Override
	public void channelChanged(byte identifier, Value value) {
		if (identifier == 0x01) {
			BooleanValue v = (BooleanValue)value;
			this.remoteDevice.pushCommand((byte) 0xA0, (byte) 0x11, 
					"0201" + ((v.getBooleanValue()) ? "C8" : "00") + "0000");
			// Get state
			this.remoteDevice.pushCommand((byte) 0xA0, (byte) 0x01, "010E");
		}
	}
}
