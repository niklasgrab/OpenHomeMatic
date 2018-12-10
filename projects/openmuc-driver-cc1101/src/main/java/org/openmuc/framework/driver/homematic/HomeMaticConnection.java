/*
 * Copyright 2016-18 ISC Konstanz
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.DriverInfoFactory;
import org.openmuc.framework.config.DriverPreferences;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.ogema.driver.homematic.HomeMaticChannel;
import org.ogema.driver.homematic.HomeMaticException;
import org.ogema.driver.homematic.data.UpdateListener;
import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.DeviceAttribute;
import org.ogema.driver.homematic.manager.DeviceChannel;
import org.ogema.driver.homematic.manager.DeviceCommand;
import org.ogema.driver.homematic.manager.devices.PowerMeter;
import org.ogema.driver.homematic.manager.devices.SwitchPlug;
import org.openmuc.framework.driver.homematic.settings.ChannelSettings;
import org.openmuc.framework.driver.homematic.settings.DeviceSettings;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeMaticConnection implements Connection, UpdateListener {
	private static final Logger logger = LoggerFactory.getLogger(HomeMaticConnection.class);

	private final DriverPreferences prefs = DriverInfoFactory.getPreferences(HomeMaticDriver.class);

	/**
	 * Interface used by {@link HomeMaticConnection} to notify the {@link HomeMaticDriver} about events
	 */
	public interface HomeMaticConnectionCallbacks {
		
		public void onDisconnect(String deviceAddress);
	}

	/**
	 * The Connections current callback object, which is used to notify of connection events
	 */
	private final HomeMaticConnectionCallbacks callbacks;

	private final Map<String, HomeMaticChannel> channels = new HashMap<String, HomeMaticChannel>();
	private final Device device;
	
	private final Map<RecordsReceivedListener, List<ChannelRecordContainer>> listenerMap = 
			new HashMap<RecordsReceivedListener, List<ChannelRecordContainer>>();

	public HomeMaticConnection(HomeMaticConnectionCallbacks callbacks, Device device, DeviceSettings settings) 
			throws ConnectionException {
		this.callbacks = callbacks;
		this.device = device;
		
		if (device instanceof PowerMeter || device instanceof SwitchPlug) {
			if (settings.hasDefaultState()) {
				BooleanValue defaultState = new BooleanValue(settings.getDefaultState());
				if (!defaultState.equals(device.deviceAttributes.get((short) 0x0001).getValue())) {
					try {
						device.channelChanged((byte) 0x01, HomeMaticPort.enocde(defaultState));
					} catch (HomeMaticException e) {
						throw new ConnectionException(e.getMessage());
					}
				}
			}
		}
	}

	@Override
	public List<ChannelScanInfo> scanForChannels(String settingsStr)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {
		
		List<ChannelScanInfo> channels = new ArrayList<ChannelScanInfo>();
		for (DeviceChannel channel : getRegisters()) {
			ValueType valueType = encodeValueType(channel.getValueType());
			
			String type = "Unknown";
			boolean write = false;
			if (channel instanceof DeviceCommand) {
				type = "command";
				write = true;
			}
			else if (channel instanceof DeviceAttribute) {
				type = "attribute";
			}
			channels.add(new ChannelScanInfo(channel.getChannelAddress(), "type:"+type.toUpperCase(), 
					type+"_"+channel.getDescription().toLowerCase(), valueType, null, true, write));
		}
		return channels;
	}

	@Override
	public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
			throws UnsupportedOperationException, ConnectionException {
		
		try {
			for (ChannelRecordContainer container : containers) {
				try {
					ChannelSettings settings = prefs.get(container.getChannelSettings(), ChannelSettings.class);
					
					HomeMaticChannel channel = getChannel(container.getChannelAddress(), settings.getType());
					Record record = HomeMaticPort.decode(channel.readRecord());
					
					container.setRecord(record);
				}
				catch (NullPointerException | ArgumentSyntaxException e) {
					container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID));	
					logger.warn("Unable to configure channel address \"{}\": {}", container.getChannelAddress(), e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			throw new ConnectionException(e);
		}
		
		return null;
	}

	@Override
	public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
			throws UnsupportedOperationException, ConnectionException {
		try {
			List<ChannelRecordContainer> listenerContainers;
			if (!listenerMap.containsKey(listener)) {
				listenerContainers = new ArrayList<ChannelRecordContainer>();
				listenerMap.put(listener, listenerContainers);
			}
			else {
				listenerContainers = listenerMap.get(listener);
			}
			for (ChannelRecordContainer container : containers) {
				if (!listenerContainers.contains(container)) {
					listenerContainers.add(container);
				}
				try {
					ChannelSettings settings = prefs.get(container.getChannelSettings(), ChannelSettings.class);
					
					HomeMaticChannel channel = getChannel(container.getChannelAddress(), settings.getType());
					channel.setEventListener(this);
				}
				catch (NullPointerException | ArgumentSyntaxException e) {
					logger.warn("Unable to configure channel address \"{}\": {}", container.getChannelAddress(), e.getMessage());
				}
			}
		} catch (Exception e) {
			throw new ConnectionException(e);
		}
	}

	@Override
	public void valueChanged(org.ogema.driver.homematic.data.TimeValue record, String address) {
		Iterator<RecordsReceivedListener> it = listenerMap.keySet().iterator();
		while (it.hasNext()) {
			RecordsReceivedListener recordReceivedListener = it.next();
			List<ChannelRecordContainer> listenerContainers = listenerMap.get(recordReceivedListener);
			List<ChannelRecordContainer> updatedContainers = new ArrayList<ChannelRecordContainer>();
			for (ChannelRecordContainer container : listenerContainers) {
				if (container.getChannelAddress().equals(address)) {
					container.setRecord(HomeMaticPort.decode(record));
					updatedContainers.add(container);
				}
			}
			if (! updatedContainers.isEmpty()) {
				recordReceivedListener.newRecords(updatedContainers);
			}
		}
		
	}
	@Override
	public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
			throws UnsupportedOperationException, ConnectionException {
		
		try {
			for (ChannelValueContainer container : containers) {
				try {
					ChannelSettings settings = prefs.get(container.getChannelSettings(), ChannelSettings.class);
					HomeMaticChannel channel = getChannel(container.getChannelAddress(), settings.getType());
					try {
						channel.writeValue(HomeMaticPort.encode(container.getValue()));
					} catch (HomeMaticException e) {
						throw new ConnectionException(e.getMessage());
					}
					
					container.setFlag(Flag.VALID);
				}
				catch (NullPointerException | ArgumentSyntaxException e) {
					logger.warn("Unable to configure channel address \"{}\": {}", container.getChannelAddress(), e.getMessage());
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			throw new ConnectionException(e);
		}
		return null;
	}

	@Override
	public void disconnect() {
		channels.clear();
		if (callbacks != null) {
			callbacks.onDisconnect(device.getAddress());
		}
	}

	private HomeMaticChannel getChannel(String identifier, String type) {
		String address = type + ":" + identifier;
		HomeMaticChannel channel = channels.get(address);
		if (channel == null) {
			channel = HomeMaticChannel.createChannel(address, device);
			channels.put(channel.getAddress(), channel);
		}
		return channel;
	}

	public List<DeviceChannel> getRegisters() {
		List<DeviceChannel> registers = new ArrayList<DeviceChannel>();
		registers.addAll(device.deviceCommands.values());
		registers.addAll(device.deviceAttributes.values());
		
		return registers;
	}

	private ValueType encodeValueType(org.ogema.driver.homematic.manager.ValueType valueType) {
		switch(valueType) {
		case BOOLEAN:
			return ValueType.BOOLEAN;
		case BYTE:
			return ValueType.BYTE;
		case BYTE_ARRAY:
			return ValueType.BYTE_ARRAY;
		case DOUBLE:
			return ValueType.DOUBLE;
		case FLOAT:
			return ValueType.FLOAT;
		case INTEGER:
			return ValueType.INTEGER;
		case LONG:
			return ValueType.LONG;
		case SHORT:
			return ValueType.SHORT;
		default:
			return ValueType.STRING;
		}
	}
}
