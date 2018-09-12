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

import java.io.IOException;

import javax.xml.bind.DatatypeConverter;

import org.ogema.core.channelmanager.driverspi.ChannelUpdateListener;
import org.ogema.core.channelmanager.driverspi.SampledValueContainer;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.DeviceCommand;

public class CommandChannel extends HomeMaticChannel {

	private Device device;
	private DeviceCommand deviceCommand;

	public CommandChannel(String address, String[] configs, Device device) {
		super(address);
		this.setDevice(device);
		
		byte[] commandIdArray = DatatypeConverter.parseHexBinary(configs[1]);
		byte commandId = commandIdArray[0];
		deviceCommand = device.getHandler().deviceCommands.get(commandId);
		deviceCommand.getIdentifier();
	}

	@Override
	public SampledValue readValue() throws IOException, UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	/**
	 * The value has to be a ByteArrayValue in reversed byte order
	 */
	@Override
	public void writeValue(Value value) throws IOException {
		// byte[] messagePayload = value.getByteArrayValue();
		// if (messagePayload == null)
		// messagePayload = emptyMessagePayload;
		deviceCommand.channelChanged(value);
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	@Override
	public void setEventListener(SampledValueContainer container, ChannelUpdateListener listener) throws IOException,
			UnsupportedOperationException {
		// throw new UnsupportedOperationException();
	}

	@Override
	public void removeUpdateListener() throws IOException, UnsupportedOperationException {
		// throw new UnsupportedOperationException();
	}

}
