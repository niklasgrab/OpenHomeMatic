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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.driverspi.ChannelLocator;
import org.ogema.core.channelmanager.driverspi.DeviceLocator;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.driver.homematic.Channel;
import org.ogema.driver.homematic.Device;
import org.ogema.driver.homematic.manager.DeviceAttribute;
import org.ogema.driver.homematic.manager.DeviceCommand;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;
import org.ogema.driver.homematic.manager.asksin.RemoteDevice;
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
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeMaticConnection extends Device implements Connection {
	private final static Logger logger = LoggerFactory.getLogger(HomeMaticConnection.class);

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
    private HomeMaticConnectionCallbacks callbacks;

	private LocalDevice localDevice;

	public HomeMaticConnection(String deviceAddress, HomeMaticConnectionCallbacks callbacks) {
		super(new DeviceLocator("null", "null", deviceAddress, null), null);
		
        this.callbacks = callbacks;
 	}

	@Override
	public List<ChannelScanInfo> scanForChannels(String settingsStr)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {
		
        logger.debug("Scanning for channels started");
        List<ChannelScanInfo> channels = new ArrayList<>();
        
        Map<Byte, DeviceCommand> deviceCommands =
				localDevice.getDevices().get(getDeviceAddress()).getSubDevice().deviceCommands;	
        RemoteDevice remoteDevice =  (RemoteDevice)localDevice.getDevices().get(getDeviceAddress());
		Iterator<DeviceCommand> it = deviceCommands.values().iterator();
		while (it.hasNext()) {
			DeviceCommand command = it.next();
			String channel = command.generateChannelAddress();
			ValueType valType = OgemaValue.encodeValueType(remoteDevice.getSubDevice().deviceCommands.get(
					command.getIdentifier()).getValueType());
		    ChannelScanInfo channelInfo = new ChannelScanInfo(channel, 
		    		"Device Type of Channel is: " + localDevice.getDevices().get(getDeviceAddress()).getDeviceType(), 
		    		valType, null);
		    
			channels.add(channelInfo);
		}
		
		Map<Short, DeviceAttribute> deviceAttributes =
				localDevice.getDevices().get(getDeviceAddress()).getSubDevice().deviceAttributes;
		Iterator<DeviceAttribute> itAttr = deviceAttributes.values().iterator();
		while (itAttr.hasNext()) {
			DeviceAttribute attribute = itAttr.next();
			String channel = attribute.generateChannelAddress();
			ValueType valType = OgemaValue.encodeValueType(remoteDevice.getSubDevice().deviceAttributes.get(
					attribute.getIdentifier()).getValueType());
		    ChannelScanInfo channelInfo = new ChannelScanInfo(channel, 
		    		"Device Type of Channel is: " + localDevice.getDevices().get(getDeviceAddress()).getDeviceType(), 
		    		valType, null);
		    
			channels.add(channelInfo);
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
					
					if (isRemoteDeviceAvailable(container.getChannelAddress())) {
		        		Channel channel = getChannel(container.getChannelAddress(), settings.getType());
		        		SampledValue readValue = channel.readValue(null);
		        		if (readValue.getValue() == null) {
		        	        logger.debug("No value received yet from device \"{}\" for channel: {}", getDeviceAddress(), channel.getChannelLocator().getChannelAddress());
		        		}
						container.setRecord(OgemaSampledValue.decode(channel.readValue(null)));
					}
					else {
						throw new ConnectionException("Corresponding device was deleted due to a pairing failure");
					}
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
	        		
	        		Channel channel = getChannel(container.getChannelAddress(), settings.getType());
					if (isRemoteDeviceAvailable(container.getChannelAddress())) {
		        		channel = getChannel(container.getChannelAddress(), settings.getType());
			        	channel.setEventListener(OgemaSampledValue.encodeContainer(container), updListener);
					} 
					else {
						channel.removeUpdateListener();
						throw new ConnectionException("Device deleted by pairing failure");
					}
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
					if (isRemoteDeviceAvailable(container.getChannelAddress())) {
		        		ChannelSettings settings = prefs.get(container.getChannelSettings(), ChannelSettings.class);
		        		Channel channel = getChannel(container.getChannelAddress(), settings.getType());
						channel.writeValue(null, OgemaValue.encode(container.getValue()));
					}
					else {
						throw new ConnectionException("Device deleted due to pairing failure");
					}
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
		close();
	}

	public void close() {
		if (localDevice != null) {
		  localDevice.getDevices().remove(this.getDeviceAddress());
		  localDevice = null;
		}
		getChannels().clear();
		if (callbacks != null) {
		  callbacks.onDisconnect(getDeviceAddress());
          callbacks = null;
		}
	}

	@Override
	public RemoteDevice getRemoteDevice() {
		return (RemoteDevice) getLocalDevice().getDevices().get(getDeviceAddress());
	}

	public LocalDevice getLocalDevice() {
		return localDevice;
	}

	public void setLocalDevice(LocalDevice localDevice) {
		this.localDevice = localDevice;
	}

	/*
	 * Check if RemoteDevice of the channel is removed in localDevice.
	 * In case of true remove the channel from the channelMap to allow 
	 * repairing the device, because in this case we have to recreate 
	 * the channel. Then the channel holds the deviceAttribute and this is
	 * changed.
	 */
	private boolean isRemoteDeviceAvailable(String channelAddress) {
		boolean retVal = localDevice.getDevices().containsKey(getDeviceAddress());
		if (!retVal) getChannels().remove(channelAddress);
		return retVal;
	}
	
	private Channel getChannel(String channelAddress, String settings) {
		Channel channel = getChannels().get(channelAddress);
    	if (channel == null) {
    		ChannelLocator channelLocator = new ChannelLocator(settings + ":" + channelAddress, getDeviceLocator());
			channel = Channel.createChannel(channelLocator, this);
			getChannels().put(channelAddress, channel);   		
    	}
    	return channel;
	}

}
