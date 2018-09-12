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
import java.util.List;

import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.driver.homematic.HomeMaticChannel;
import org.ogema.driver.homematic.HomeMaticDevice;
import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.DeviceAttribute;
import org.ogema.driver.homematic.manager.DeviceChannel;
import org.ogema.driver.homematic.manager.DeviceCommand;
import org.ogema.driver.homematic.manager.DeviceHandler;
import org.ogema.driver.homematic.manager.devices.PowerMeter;
import org.ogema.driver.homematic.manager.devices.SwitchPlug;
import org.ogema.port.ChannelUpdateListener;
import org.ogema.port.OgemaSampledValue;
import org.ogema.port.OgemaValue;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.DriverInfoFactory;
import org.openmuc.framework.config.DriverPreferences;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.openmuc.framework.driver.homematic.settings.ChannelSettings;
import org.openmuc.framework.driver.homematic.settings.DeviceSettings;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeMaticConnection extends HomeMaticDevice implements Connection {
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

	public HomeMaticConnection(HomeMaticConnectionCallbacks callbacks, Device device, DeviceSettings settings) 
			throws ConnectionException {
		super(device);
		this.callbacks = callbacks;
		
		DeviceHandler handler = device.getHandler();
		if (handler instanceof PowerMeter || handler instanceof SwitchPlug) {
			if (settings.hasDefaultState()) {
				BooleanValue defaultState = new BooleanValue(settings.getDefaultState());
				if (!defaultState.equals(handler.deviceAttributes.get((short) 0x0001).getValue())) {
					handler.channelChanged((byte) 0x01, defaultState);
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
			channels.add(new ChannelScanInfo(channel.getIdentifier(), "type:"+type.toUpperCase(), 
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
					SampledValue readValue = channel.readValue();
					if (readValue.getValue() == null) {
						logger.debug("No value received yet from device \"{}\" for channel: {}", device.getAddress(), channel.getAddress());
					}
					container.setRecord(OgemaSampledValue.decode(channel.readValue()));
				}
				catch (NullPointerException | ArgumentSyntaxException e) {
					container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID));					
					logger.warn("Unable to configure channel address \"{}\": {}", container.getChannelAddress(), e.getMessage());
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
		ChannelUpdateListener updListener = new ChannelUpdateListener(containers, listener);
		try {
			for (ChannelRecordContainer container : containers) {
				try {
					ChannelSettings settings = prefs.get(container.getChannelSettings(), ChannelSettings.class);
					
					HomeMaticChannel channel = getChannel(container.getChannelAddress(), settings.getType());
					channel.setEventListener(OgemaSampledValue.encodeContainer(container), updListener);
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
	public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
			throws UnsupportedOperationException, ConnectionException {
		
		try {
			for (ChannelValueContainer container : containers) {
				try {
					ChannelSettings settings = prefs.get(container.getChannelSettings(), ChannelSettings.class);
					HomeMaticChannel channel = getChannel(container.getChannelAddress(), settings.getType());
					channel.writeValue(OgemaValue.encode(container.getValue()));
					
					container.setFlag(Flag.VALID);
				}
				catch (NullPointerException | ArgumentSyntaxException e) {
					logger.warn("Unable to configure channel address \"{}\": {}", container.getChannelAddress(), e.getMessage());
				}
			}
		} catch (IOException e) {
			throw new ConnectionException(e);
		}
		return null;
	}

	@Override
	public void disconnect() {
		getChannels().clear();
		if (callbacks != null) {
			callbacks.onDisconnect(device.getAddress());
		}
	}

	private HomeMaticChannel getChannel(String identifier, String type) {
		String address = type + ":" + identifier;
		HomeMaticChannel channel = getChannel(address);
		if (channel == null) {
			channel = HomeMaticChannel.createChannel(address, this);
			addChannel(channel);
		}
		return channel;
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
