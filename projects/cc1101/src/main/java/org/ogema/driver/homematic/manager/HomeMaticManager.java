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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ogema.driver.homematic.HomeMaticException;
import org.ogema.driver.homematic.manager.devices.DeviceDescriptor;

public class HomeMaticManager {
	private MessageHandler messageHandler;

	private final DeviceDescriptor descriptor;

	// null = nothing; 0000000000 = pair all; XXXXXXXXXX = pair serial number
	private String pairing = null; 

	private String serial = "0000000000";
	private String firmware = "0.0";

	private final Map<String, Device> devices;

	public HomeMaticManager() {
		this.devices = new ConcurrentHashMap<String, Device>();
		this.descriptor = new DeviceDescriptor();
		messageHandler = new MessageHandler(this);
	}

	public Device addDevice(String address, String type, String serial) throws HomeMaticException {
		if (! messageHandler.isReady()) {
			throw new HomeMaticException("Connection not yet established!");
		}
		Device device = Device.createPairedDevice(descriptor, messageHandler, address, type, serial);
		addDevice(device);
		
		return device;
	}

	public Device addDevice(String address, String type) throws HomeMaticException {
		return addDevice(address, type, null);
	}

	public void addDevice(Device device) {
		devices.put(device.getAddress(), device);
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

	public String getSerial() {
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

	public void close() {
		messageHandler.stop();
	}
}
