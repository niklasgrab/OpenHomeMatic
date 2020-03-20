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
package org.ogema.driver.homematic;

import java.io.Closeable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ogema.driver.homematic.com.message.MessageHandler;
import org.ogema.driver.homematic.device.Device;
import org.ogema.driver.homematic.device.DeviceDescriptor;
import org.ogema.driver.homematic.device.Device.DeviceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeMaticManager implements Closeable {
	private final Logger logger = LoggerFactory.getLogger(HomeMaticManager.class);

	private final MessageHandler handler;
	private final DeviceDescriptor descriptor;

	// null = nothing; 0000000000 = pair all; XXXXXXXXXX = pair serial number
	private String pairing = null; 

	private String serial = "0000000000";
	private String firmware = "0.0";

	private final Map<String, Device> devices;

	public HomeMaticManager() {
		this.devices = new ConcurrentHashMap<String, Device>();
		this.descriptor = new DeviceDescriptor();
		this.handler = new MessageHandler(this);
	}

	public Device addDevice(String id, String type) throws HomeMaticException {
		if (!handler.isReady()) {
			throw new HomeMaticException("Connection not yet established!");
		}
		Device device = Device.create(descriptor, handler, id, type, serial);
		device.setState(DeviceState.PAIRED);
		device.activate();
		devices.put(device.getId(), device);
		logger.info("Add paired {} device for address {}", descriptor.getName(type), id);
		
		return device;
	}

	public void addDevice(Device device) {
		devices.put(device.getId(), device);
	}

	public boolean hasDevice(String address) {
		return devices.containsKey(address);
	}

	public Device getDevice(String address) {
		return devices.get(address);
	}

	public Map<String, Device> getDevices() {
		return devices;
	}

	public String getSerialPort() {
		return serial;
	}

	public void setSerial(String serial) {
		this.serial = serial;
	}

	public String getFirmware() {
		return firmware;
	}

	public void setFirmware(String firmware) {
		this.firmware = firmware;
	}

	public DeviceDescriptor getDeviceDescriptor() {
		return descriptor;
	}

	/**
	 * enable pairing with a specific device. 
	 * @param val serial number of the device as 5 Bytes Hex 
	 */
	public void setPairing(String val) {
		this.pairing = val;
	}

	public String getPairing() {
		return this.pairing;
	}

	@Override
	public void close() {
		handler.stop();
	}
}
