/*
 * Copyright 2011-18 Fraunhofer ISE
 *
 * This file is part of OpenMUC.
 * For more information visit http://www.openmuc.org
 *
 * OpenMUC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenMUC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenMUC.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.driver.homematic.device;

import java.util.LinkedList;
import java.util.List;

import org.ogema.driver.homematic.device.Device;
import org.ogema.driver.homematic.device.DeviceChannel;
import org.ogema.driver.homematic.device.DeviceCommand;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.ChannelScanner;
import org.openmuc.framework.driver.spi.ConnectionException;

public class DeviceChannels extends ChannelScanner {

	private final List<DeviceChannel> channels = new LinkedList<DeviceChannel>();

	public DeviceChannels(Device device) {
		channels.addAll(device.getCommands());
		channels.addAll(device.getAttributes());
	}

	@Override
	public List<ChannelScanInfo> doScan() throws ArgumentSyntaxException, ScanException, ConnectionException {
        List<ChannelScanInfo> channels = new LinkedList<ChannelScanInfo>();
        for (DeviceChannel channel : this.channels) {
			boolean read;
			boolean write;
			String type = "Unknown";
			if (channel instanceof DeviceCommand) {
				read = false;
				write = true;
				type = "Command";
			}
			else { //if (channel instanceof DeviceAttribute) {
				read = true;
				write = false;
				type = "Attribute";
			}
    		channels.add(new ChannelScanInfo(channel.getKey(), 
    				"type:"+type.toUpperCase(), 
    				type+" "+channel.getName().toLowerCase(), 
    				decode(channel.getType()), null, read, write));
        }
        return channels;
	}

	private ValueType decode(org.ogema.driver.homematic.data.ValueType valueType) {
		switch(valueType) {
		case BOOLEAN:
			return ValueType.BOOLEAN;
		case SHORT:
			return ValueType.SHORT;
		case INTEGER:
			return ValueType.INTEGER;
		case LONG:
			return ValueType.LONG;
		case FLOAT:
			return ValueType.FLOAT;
		case DOUBLE:
			return ValueType.DOUBLE;
		default:
			return ValueType.STRING;
		}
	}

}
