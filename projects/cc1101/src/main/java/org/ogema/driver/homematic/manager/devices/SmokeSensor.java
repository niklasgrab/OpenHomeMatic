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
package org.ogema.driver.homematic.manager.devices;

import org.ogema.driver.homematic.HomeMaticConnectionException;
import org.ogema.driver.homematic.data.BooleanValue;
import org.ogema.driver.homematic.data.FloatValue;
import org.ogema.driver.homematic.data.Value;
import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.DeviceAttribute;
import org.ogema.driver.homematic.manager.MessageHandler;
import org.ogema.driver.homematic.manager.ValueType;
import org.ogema.driver.homematic.manager.messages.CommandMessage;
import org.ogema.driver.homematic.manager.messages.StatusMessage;
import org.ogema.driver.homematic.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmokeSensor extends Device {

	private final Logger logger = LoggerFactory.getLogger(SmokeSensor.class);

	public SmokeSensor(DeviceDescriptor descriptor, MessageHandler messageHandler, String address, String deviceKey, String serial) 
			throws HomeMaticConnectionException {
		super(descriptor, messageHandler, address, deviceKey, serial);
	}

	@Override
	protected void configureChannels() {
		deviceAttributes.put((short) 0x0001, new DeviceAttribute((short) 0x0001, "Temperature", true, true, ValueType.BOOLEAN));
		deviceAttributes.put((short) 0x0002, new DeviceAttribute((short) 0x0002, "BatteryStatus", true, true, ValueType.FLOAT));
	}

	@Override
	protected void parseValue(StatusMessage msg) {
		if (msg.type == 0x41) {
			long status = Converter.toLong(msg.data[2]);
			long err = Converter.toLong(msg.data[0]);

			String err_str = ((err & 0x80) > 0) ? "low" : "ok";
			float batt = ((err & 0x80) > 0) ? 5 : 95;
			logger.debug("State of Battery: " + err_str);
			deviceAttributes.get((short) 0x0003).setValue(new FloatValue(batt));

			if (status > 1) {
				logger.debug("Smoke Alert: true");
				deviceAttributes.get((short) 0x0001).setValue(new BooleanValue(true));
			}
			else {
				logger.debug("Smoke Alert: false");
				deviceAttributes.get((short) 0x0001).setValue(new BooleanValue(false));
			}
		}
	}

	@Override
	public void channelChanged(byte identifier, Value value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void parseMessage(StatusMessage msg, CommandMessage cmd, Device device) {
		byte msgType = msg.type;
		byte contentType = msg.data[0];

		if ((msgType == 0x10 && (contentType == 0x02) || (contentType == 0x03))) {
			// Configuration response Message
			parseConfig(msg, cmd);
		}
		else {
			parseValue(msg);
		}
	}
}
