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

public abstract class DeviceChannel {

	protected final String key;
	protected final String name;

	protected final ValueType type;

	public DeviceChannel(String id, String name, ValueType type) {
		this.key = id;
		this.name = name;
		this.type = type;
	}

	public String getKey() {
		return key;
	}

	public String getName() {
		return name;
	}

	public ValueType getType() {
		return type;
	}

}
