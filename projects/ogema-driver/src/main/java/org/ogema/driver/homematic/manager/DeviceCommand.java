/**
 * This file is part of OGEMA.
 *
 * OGEMA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * OGEMA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OGEMA. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ogema.driver.homematic.manager;

import org.ogema.core.channelmanager.measurements.Value;

/**
 * This class represents a command of a HomeMatic Device.
 * 
 */
public class DeviceCommand extends DeviceChannel {
	protected final byte identifier;
	protected final String address;
	protected DeviceHandler device;

	public DeviceCommand(DeviceHandler device, byte identifier, String description, boolean mandatory, ValueType valueType) {
		super(description, mandatory, valueType);
		this.identifier = identifier;
		this.address = "COMMAND:"+getIdentifier();
		this.device = device;
	}

	@Override
	public String getAddress() {
		return address;
	}

	@Override
	public String getIdentifier() {
		StringBuilder id = new StringBuilder();
		id.append(Integer.toHexString(identifier & 0xff));
		switch (id.length()) {
		case 0:
			id.append("00");
			break;
		case 1:
			id.insert(id.length() - 1, "0");
			break;
		}
		return id.toString();
	}

	public void channelChanged(Value value) {
		this.device.channelChanged(identifier, value);
	}

}
