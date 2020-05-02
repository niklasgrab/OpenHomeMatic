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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenHomeMatic.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.driver.homematic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.driver.homematic.HomeMaticException;
import org.ogema.driver.homematic.device.DeviceAttribute;
import org.ogema.driver.homematic.device.DeviceCommand;
import org.ogema.driver.homematic.device.DeviceListener;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.driver.Device;
import org.openmuc.framework.driver.homematic.device.DeviceChannel;
import org.openmuc.framework.driver.homematic.device.DeviceChannels;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;

public class HomeMaticConnection extends Device<DeviceChannel> implements DeviceListener {

	private Map<String, DeviceChannel> listenedChannels = new HashMap<String, DeviceChannel>();
	private RecordsReceivedListener listener;

	final org.ogema.driver.homematic.device.Device device;

	public HomeMaticConnection(org.ogema.driver.homematic.device.Device device) {
		this.device = device;
	}

	@Override
    protected DeviceChannels onCreateScanner(String settings) 
            throws ArgumentSyntaxException, ScanException, ConnectionException {
		
        return new DeviceChannels(device);
    }

	@Override
    public void onStartListening(List<DeviceChannel> channels, RecordsReceivedListener listener)
            throws UnsupportedOperationException, ConnectionException {
		
		this.listener = listener;
		this.listenedChannels.clear();
		for (DeviceChannel channel : channels) {
			listenedChannels.put(channel.getAddress(), channel);
		}
		device.setListener(this);
	}

	@Override
	public void onAttributesChanged(Collection<DeviceAttribute> attributes) {
		List<ChannelRecordContainer> containers = new ArrayList<ChannelRecordContainer>();
		for (DeviceAttribute attribute : attributes) {
			DeviceChannel channel = listenedChannels.get(attribute.getKey());
			containers.add(channel);
			channel.setRecord(attribute);
		}
		listener.newRecords(containers);
	}

	@Override
    public Object onRead(List<DeviceChannel> channels, Object containerListHandle, String samplingGroup)
            throws UnsupportedOperationException, ConnectionException {
		
		for (DeviceChannel channel : channels) {
			if (channel.getType().toUpperCase().equals("COMMAND")) {
				throw new UnsupportedOperationException("Unable to read device Command "+channel.getAddress());
			}
			channel.setRecord(device.getAttribute(channel.getAddress()));
		}
		return null;
	}

	@Override
    protected Object onWrite(List<DeviceChannel> channels, Object containerListHandle)
            throws UnsupportedOperationException, ConnectionException {

		try {
			for (DeviceChannel channel : channels) {
				if (channel.getType().toUpperCase().equals("ATTRIBUTE")) {
					throw new UnsupportedOperationException("Unable to read device Attribute "+channel.getAddress());
				}
				DeviceCommand command = device.getCommand(channel.getAddress());
				device.sendCommand(command, channel.encodeValue());
				channel.setFlag(Flag.VALID);
			}
		} catch (HomeMaticException e) {
			throw new ConnectionException(e);
		}
		return null;
	}

	@Override
	protected void onDisconnect() {
		device.removeListener();
	}

}
