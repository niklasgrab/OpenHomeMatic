/*
 * Copyright 2016-20 ISC Konstanz
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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenHomeMatic.	If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.driver.homematic.device;

import org.ogema.driver.homematic.device.DeviceAttribute;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.Channel;
import org.openmuc.framework.options.Address;
import org.openmuc.framework.options.Setting;

public class DeviceChannel extends Channel {

	@Address(id = "id",
			name = "Identifier",
			description = "The unique identifier of the data channel")
	private String address;

	@Setting(id = "type",
			name = "Type",
			description = "The type of the register:<br>" +
							"<ol>" +
								"<li>Attribute channels from which can be read.</li>" +
								"<li>Command channels which can be set</li>" +
							"</ol>",
			valueSelection = "ATTRIBUTE:Attribute,COMMAND:Command",
			valueDefault = "ATTRIBUTE")
	private String type;

	public String getAddress() {
		return address;
	}

	public String getType() {
		return type;
	}

	public void setRecord(DeviceAttribute attribute) {
		if (attribute == null) {
			setFlag(Flag.DRIVER_ERROR_CHANNEL_NOT_ACCESSIBLE);
			return;
		}
		Value value = decodeValue(attribute.getValue());
		if (value == null) {
			setFlag(Flag.NO_VALUE_RECEIVED_YET);
			return;
		}
		setRecord(new Record(value, attribute.getTimestamp(), Flag.VALID));
	}

	public Value decodeValue(org.ogema.driver.homematic.data.Value val) {
		if (val == null) {
			return null;
		}
		String name = val.getClass().getSimpleName();
		Value value = null;
		switch (name) {
			case "BooleanValue":
				value = decode((org.ogema.driver.homematic.data.BooleanValue) val);
				break;
			case "FloatValue":
				value = decode((org.ogema.driver.homematic.data.FloatValue) val);
				break;
			default:
				value = decode((org.ogema.driver.homematic.data.StringValue) val);
				break;				
		}
		return value;
	}

	private BooleanValue decode(org.ogema.driver.homematic.data.BooleanValue val) {
		return new BooleanValue(val.asBoolean());
	}

	private FloatValue decode(org.ogema.driver.homematic.data.FloatValue val) {
		return new FloatValue(val.asFloat());
	}

	private StringValue decode(org.ogema.driver.homematic.data.StringValue val) {
		return new StringValue(val.asString());
	}

	public org.ogema.driver.homematic.data.Value encodeValue() {
		String name = getValue().getClass().getSimpleName();
		org.ogema.driver.homematic.data.Value value = null;
		switch (name) {
			case "BooleanValue":
				value = enocde((BooleanValue) getValue());
				break;
			case "FloatValue":
				value = encode((FloatValue) getValue());
				break;
			default:
				value = encode((StringValue) getValue());
				break;				
		}
		return value;
	}

	private org.ogema.driver.homematic.data.BooleanValue enocde(BooleanValue val) {
		return new org.ogema.driver.homematic.data.BooleanValue(val.asBoolean());
	}

	private org.ogema.driver.homematic.data.FloatValue encode(FloatValue val) {
		return new org.ogema.driver.homematic.data.FloatValue(val.asFloat());
	}

	private org.ogema.driver.homematic.data.StringValue encode(StringValue val) {
		return new org.ogema.driver.homematic.data.StringValue(val.asString());
	}

}
