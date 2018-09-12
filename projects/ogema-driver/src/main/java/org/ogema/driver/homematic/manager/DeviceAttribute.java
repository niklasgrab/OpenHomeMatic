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
import org.ogema.driver.homematic.AttributeChannel;

public class DeviceAttribute extends DeviceChannel {
	protected final short identifier;
	protected final String address;
	protected final boolean readOnly;
	protected Value value;
	protected long valueTimestamp;
	protected boolean haslistener = false;

	protected AttributeChannel attributeChannel;

	public DeviceAttribute(short identifier, String description, boolean readOnly, boolean mandatory, ValueType valueType) {
		super(description, mandatory, valueType);
		this.identifier = identifier;
		this.address = "ATTRIBUTE:"+getIdentifier();
		this.readOnly = readOnly;
	}

	@Override
	public String getAddress() {
		return address;
	}

	@Override
	public String getIdentifier() {
		StringBuilder id = new StringBuilder();
		id.append(Integer.toHexString(identifier & 0xffff));
		switch (id.length()) {
		case 0:
			id.append("0000");
			break;
		case 1:
			id.insert(id.length() - 1, "000");
			break;
		case 2:
			id.insert(id.length() - 2, "00");
			break;
		case 3:
			id.insert(id.length() - 3, "0");
			break;
		}
		return id.toString();
	}

	public short getShortId() {
		return identifier;
	}

	public boolean readOnly() {
		return readOnly;
	}

	public Value getValue() {
		return value;
	}

	public void setValue(Value value) {
		this.value = value;
		valueTimestamp = System.currentTimeMillis();
		if (haslistener) {
			attributeChannel.updateListener();
		}
	}

	public long getValueTimestamp() {
		return valueTimestamp;
	}

	public void setChannel(AttributeChannel attributeChannel) {
		this.attributeChannel = attributeChannel;
	}

	public void setListener(boolean b) {
		haslistener = b;
	}
}
