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

/**
 * This class represents a command of a HomeMatic Device.
 * 
 */
public class DeviceCommand extends DeviceChannel {
	protected final byte identifier;

	public DeviceCommand(Device device, byte identifier, String description, boolean mandatory, ValueType valueType) {
		super(description, mandatory, valueType);
		this.identifier = identifier;
	}

	@Override
	public String getChannelAddress() {
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
	
	public byte getIdentifier() {
		return identifier;
	}
}
