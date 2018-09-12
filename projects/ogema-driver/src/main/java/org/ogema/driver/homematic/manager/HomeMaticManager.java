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

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ogema.driver.homematic.connection.Connection;
import org.ogema.driver.homematic.connection.ConnectionType;
import org.ogema.driver.homematic.connection.CulConnection;
import org.ogema.driver.homematic.connection.SccConnection;
import org.ogema.driver.homematic.manager.messages.CommandMessage;
import org.ogema.driver.homematic.manager.messages.StatusMessage;
import org.ogema.driver.homematic.tools.DeviceDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HomeMaticManager {
	private final Logger logger = LoggerFactory.getLogger(HomeMaticManager.class);

	private final String CONNECTION_INTERFACE = "org.ogema.driver.homematic.interface";
	private final String CONNECTION_DEFAULT = "SCC";

	private Connection connection;
	private OutputHandler output;
	private InputHandler input;

	private final DeviceDescriptor descriptor;

	// null = nothing; 0000000000 = pair all; XXXXXXXXXX = pair serial number
	private String pairing = null; 

	private String id = null;
	private String serial = "0000000000";
	private String firmware = "0.0";

	private final Map<String, Device> devices;

	public HomeMaticManager() {
		this.devices = new ConcurrentHashMap<String, Device>();
		this.descriptor = new DeviceDescriptor();
		try {
			initialize();
			
		} catch (Exception e) {
			logger.error("Error while initializing manager: " + e.getMessage());
		}
	}

	protected void initialize() throws IllegalArgumentException, IOException {
		ConnectionType type = ConnectionType.valueOf(System.getProperty(CONNECTION_INTERFACE, CONNECTION_DEFAULT).toUpperCase());
		switch(type) {
		case CUL:
			connection = new CulConnection();
			break;
		case SCC:
			connection = new SccConnection();
			break;
		}
		output = new OutputHandler(this);
		input = new InputHandler(this);
		input.start();
		
		connection.open();
	}

	public Device addDevice(String address, String type, String serial) {
		Device device = new Device(this, address, type, serial);
		addDevice(device);
		
		return device;
	}

	public Device addDevice(String address, String type) {
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

	public boolean isReady() {
		return this.id != null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
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

	public void onReceivedMessage(StatusMessage message) {
		output.onReceivedMessage(message);
	}

	public void sendMessage(Device device, byte flag, byte type, String data) {
		CommandMessage cmdMessage = new CommandMessage(device, id, flag, type, data);
		output.sendMessage(cmdMessage);
	}

	public void sendMessage(Device device, byte flag, byte type, byte[] data) {
		CommandMessage cmdMessage = new CommandMessage(device, id, flag, type, data);
		output.sendMessage(cmdMessage);
	}

	public Object getReceivedLock() {
		return connection.getReceivedLock();
	}

	public boolean hasReceivedFrames() {
		return connection.hasFrames();
	}

	public byte[] getReceivedFrame() {
		return connection.getReceivedFrame();
	}

	public void sendFrame(byte[] frame) {
		connection.sendFrame(frame);
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
		connection.close();
		input.stop();
	}
}
