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
package org.ogema.driver.homematic.device;

import org.ogema.driver.homematic.data.ValueType;

/**
 * This class represents a command of a HomeMatic Device.
 * 
 */
public class DeviceCommand extends DeviceChannel {

	protected final byte id;

	public DeviceCommand(byte id, String name, ValueType type) {
		super(parseKey(id), name, type);
		this.id = id;
	}

	public static String parseKey(short id) {
		StringBuilder builder = new StringBuilder();
		builder.append(Integer.toHexString(id & 0xff));
		switch (builder.length()) {
		case 0:
			builder.append("00");
			break;
		case 1:
			builder.insert(builder.length() - 1, "0");
			break;
		}
		return builder.toString();
	}

	public byte getId() {
		return id;
	}

}
