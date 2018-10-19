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
package org.ogema.driver.homematic;

import java.io.IOException;

import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.ogema.driver.homematic.manager.Device;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;


/**
 * Each channel represents a Homematic Command/Attribute
 * 
 */
public abstract class HomeMaticChannel {

	protected final String address;

	protected HomeMaticChannel(String address) {
		this.address = address;
	}

	public String getAddress() {
		return address;
	}

	public static HomeMaticChannel createChannel(String address, Device device) {
		String[] configs = address.split(":");
		switch (configs[0]) {
		case "COMMAND":
			return new CommandChannel(address, configs, device);
		case "ATTRIBUTE":
			return new AttributeChannel(address, configs, device);
		default:
			break;
		}
		throw new NullPointerException("Unable to create Channel for address: " + address);
	}

	abstract public Record readRecord() throws IOException, UnsupportedOperationException;

	abstract public void writeValue(Value value) throws IOException,
			UnsupportedOperationException;

	abstract public void setEventListener(ChannelRecordContainer container, RecordsReceivedListener listener)
			throws IOException, UnsupportedOperationException;

	abstract public void removeUpdateListener() throws IOException, UnsupportedOperationException;
}
