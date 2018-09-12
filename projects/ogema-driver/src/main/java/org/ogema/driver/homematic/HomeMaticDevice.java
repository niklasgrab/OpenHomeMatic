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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.DeviceChannel;

public class HomeMaticDevice {
	protected final Device device;
	protected final Map<String, HomeMaticChannel> channels = new HashMap<String, HomeMaticChannel>();

	public HomeMaticDevice(Device device) {
		this.device = device;
	}

	public void addChannel(HomeMaticChannel channel) {
		channels.put(channel.getAddress(), channel);
	}

	public void removeChannel(HomeMaticChannel channel) {
		channels.remove(channel.getAddress());
	}

	public HomeMaticChannel getChannel(String address) {
		return channels.get(address);
	}

	public Map<String, HomeMaticChannel> getChannels() {
		return channels;
	}

	public List<DeviceChannel> getRegisters() {
		List<DeviceChannel> registers = new ArrayList<DeviceChannel>();
		registers.addAll(device.getHandler().deviceCommands.values());
		registers.addAll(device.getHandler().deviceAttributes.values());
		
		return registers;
	}

	public String geAddress() {
		return device.getAddress();
	}

	public Device getDevice() {
		return device;
	}

}
