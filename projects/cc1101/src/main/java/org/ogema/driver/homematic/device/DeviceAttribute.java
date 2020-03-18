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

import org.ogema.driver.homematic.data.BooleanValue;
import org.ogema.driver.homematic.data.FloatValue;
import org.ogema.driver.homematic.data.StringValue;
import org.ogema.driver.homematic.data.Value;
import org.ogema.driver.homematic.data.ValueType;

public class DeviceAttribute extends DeviceChannel {

	protected final short id;

	protected Long timestamp;
	protected Value value;

	public DeviceAttribute(short id, String name, ValueType type) {
		super(parseKey(id), name, type);
		this.id = id;
	}

	public static String parseKey(short id) {
		StringBuilder builder = new StringBuilder();
		builder.append(Integer.toHexString(id & 0xffff));
		switch (builder.length()) {
		case 0:
			builder.append("0000");
			break;
		case 1:
			builder.insert(builder.length() - 1, "000");
			break;
		case 2:
			builder.insert(builder.length() - 2, "00");
			break;
		case 3:
			builder.insert(builder.length() - 3, "0");
			break;
		}
		return builder.toString();
	}

	public short getId() {
		return id;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Object obj) {
		Value value;
		switch(type) {
		case BOOLEAN:
			value = new BooleanValue((boolean) obj);
			break;
		case SHORT:
		case INTEGER:
		case LONG:
		case FLOAT:
		case DOUBLE:
			// TODO: Implement remaining value types
			value = new FloatValue((float) obj);
			break;
		default:
			value = new StringValue(String.valueOf(obj));
			break;
		}
		setValue(value);
	}

	public void setValue(Value value) {
		this.timestamp = System.currentTimeMillis();
		this.value = value;
	}

}
