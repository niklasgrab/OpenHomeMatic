/*
 * Copyright 2016-17 ISC Konstanz
 *
 * This file is part of OpenSkeleton.
 * For more information visit https://github.com/isc-konstanz/OpenSkeleton.
 *
 * OpenSkeleton is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenSkeleton is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenSkeleton.  If not, see <http://www.gnu.org/licenses/>.
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
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.ValueType;
import org.ogema.core.channelmanager.driverspi.ChannelLocator;
import org.ogema.core.channelmanager.driverspi.DeviceLocator;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.driver.homematic.Channel;
import org.ogema.driver.homematic.manager.DeviceAttribute;
import org.ogema.driver.homematic.manager.DeviceCommand;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;
import org.ogema.driver.homematic.manager.asksin.RemoteDevice;
import org.ogema.port.OgemaSampledValue;
import org.ogema.port.OgemaValue;
import org.openmuc.framework.driver.homematic.options.HomeMaticChannelPreferences;
import org.openmuc.framework.driver.homematic.options.HomeMaticDriverInfo;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HomeMaticConnection implements Connection {

	private final static Logger logger = LoggerFactory.getLogger(HomeMaticConnection.class);
	
	private final String deviceAddress;
	
	private final Map<String, Channel> channelMap;

	private LocalDevice localDevice;

	public HomeMaticConnection(String deviceAddress) {
		this.deviceAddress = deviceAddress;
		channelMap = new HashMap<String, Channel>();
 	}

	@Override
	public List<ChannelScanInfo> scanForChannels(String settingsStr)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {
		
        logger.debug("#### scan for channels called. settings: " + settingsStr);

        List<ChannelScanInfo> chanScanInf = new ArrayList<>();
        
        Map<Byte, DeviceCommand> deviceCommands =
				localDevice.getDevices().get(deviceAddress).getSubDevice().deviceCommands;	
        RemoteDevice remoteDevice =  (RemoteDevice)localDevice.getDevices().get(deviceAddress);
		Iterator<DeviceCommand> it = deviceCommands.values().iterator();
		while (it.hasNext()) {
			DeviceCommand command = it.next();
			String channel = command.generateChannelAddress();
			ValueType valType = OgemaValue.encodeValueType(remoteDevice.getSubDevice().deviceCommands.get(
					command.getIdentifier()).getValueType());
		    ChannelScanInfo channelInfo = new ChannelScanInfo(channel, 
		    		"Device Type of Channel is: " + localDevice.getDevices().get(deviceAddress).getDeviceType(), 
		        valType, null);
	        logger.debug("#### channel added : " + command.getChannelAddress());
			chanScanInf.add(channelInfo);
		}
		
		Map<Short, DeviceAttribute> deviceAttributes =
				localDevice.getDevices().get(deviceAddress).getSubDevice().deviceAttributes;
		Iterator<DeviceAttribute> itAttr = deviceAttributes.values().iterator();
		while (itAttr.hasNext()) {
			DeviceAttribute attribute = itAttr.next();
			String channel = attribute.generateChannelAddress();
			ValueType valType = OgemaValue.encodeValueType(remoteDevice.getSubDevice().deviceAttributes.get(
					attribute.getIdentifier()).getValueType());
		    ChannelScanInfo channelInfo = new ChannelScanInfo(channel, 
		    		"Device Type of Channel is: " + localDevice.getDevices().get(deviceAddress).getDeviceType(), 
		        valType, null);
	        logger.debug("#### channel added : " + attribute.getAttributeName());
			chanScanInf.add(channelInfo);
		}
		
        logger.debug("#### scan for channels finished.");
				
		return chanScanInf;
	}

	@Override
	public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
			throws UnsupportedOperationException, ConnectionException {
		
    	try {
	        for (ChannelRecordContainer container : containers) {
	        	try {
					HomeMaticChannelPreferences preferences = HomeMaticDriverInfo.getInfo().getChannelPreferences(container);
	        		Channel channel = getChannel(container.getChannelAddress(), preferences.getType());
	        		SampledValue readValue = channel.readValue(null);
	        		if (readValue.getValue() == null) {
	        	        logger.debug("No Value received yet from: " + deviceAddress + " for " + channel.getChannelLocator().getChannelAddress());
	        		}
					container.setRecord(OgemaSampledValue.decode(channel.readValue(null)));
	        	}
	        	catch (NullPointerException | IllegalArgumentException e) {
	        		container.setRecord(new Record(Flag.DRIVER_ERROR_CHANNEL_ADDRESS_SYNTAX_INVALID));	        		
	        	}
	        }
		} catch (Exception e) {
            throw new ConnectionException("channel not found");
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
					HomeMaticChannelPreferences preferences = HomeMaticDriverInfo.getInfo().getChannelPreferences(container);
	        		Channel channel = getChannel(container.getChannelAddress(), preferences.getType());
		        	channel.setEventListener(OgemaSampledValue.encodeContainer(container), updListener);
	        	}
	        	catch (NullPointerException | IllegalArgumentException e) {
	        		throw new UnsupportedOperationException("Channel not found", e);	        		
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
					HomeMaticChannelPreferences preferences = HomeMaticDriverInfo.getInfo().getChannelPreferences(container);
					Channel channel = getChannel(container.getChannelAddress(), preferences.getType());
					channel.writeValue(null, OgemaValue.encode(container.getValue()));
	        	}
	        	catch (NullPointerException | IllegalArgumentException | ArgumentSyntaxException e) {
	        		throw new UnsupportedOperationException("Channel not found", e);	        		
	        	}
			}
		} catch (IOException e) {
			throw new ConnectionException(e);
		}
		return null;
	}

	@Override
	public void disconnect() {
		
		close();
		
	}

	public void close() {
		if (localDevice!=null) {
		  localDevice.getDevices().remove(this.deviceAddress);
		}
	}

	public LocalDevice getLocalDevice() {
		return localDevice;
	}

	public void setLocalDevice(LocalDevice localDevice) {
		this.localDevice = localDevice;
	}
	
	
	private Channel getChannel(String channelAddress, String settings) {
		Channel channel = channelMap.get(channelAddress);
    	if (channel == null) {
			DeviceLocator deviceLocator = new DeviceLocator("blabla", "blabla", deviceAddress, null);
			Device dev = new Device(deviceLocator, this);
    		ChannelLocator channelLocator = new ChannelLocator(settings + ":" + channelAddress, dev.getDeviceLocator());
			channel = Channel.createChannel(channelLocator, dev);
			channelMap.put(channelAddress, channel);   		
    	}
    	return channel;
	}

}
