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
package org.ogema.driver.homematic.device;

import org.ogema.driver.homematic.HomeMaticException;
import org.ogema.driver.homematic.com.message.CommandMessage;
import org.ogema.driver.homematic.com.message.MessageHandler;
import org.ogema.driver.homematic.com.message.StatusMessage;
import org.ogema.driver.homematic.data.TypeConverter;
import org.ogema.driver.homematic.data.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreeStateSensor extends Device {

	private final Logger logger = LoggerFactory.getLogger(ThreeStateSensor.class);
	// otherwise we assume it is a water sensor
	private final boolean isDoorWindowSensor;

	public ThreeStateSensor(DeviceDescriptor descriptor, MessageHandler handler, String address, String type, String serial, 
			boolean isDoorWindowSensor) throws HomeMaticException {
		super(descriptor, handler, address, type, serial);
		this.isDoorWindowSensor = isDoorWindowSensor;
		addAttribute(0x0001, isDoorWindowSensor ? "WindowStatus" : "HighWater", ValueType.STRING);
		addAttribute(0x0002, "BatteryStatus", ValueType.FLOAT);
	}

	@Override
	public void parseMessage(StatusMessage msg, CommandMessage cmd) {
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

	@Override
	protected void parseValue(StatusMessage msg) {
		long state = 0;
		long err = 0;
		String stateStr = "";
		float batt;

		if ((msg.type == 0x10 && msg.data[0] == 0x06) || (msg.type == 0x02 && msg.data[0] == 0x01)) {
			state = TypeConverter.toLong(msg.data[1]);
			err = TypeConverter.toLong(msg.data[2]);

		}
		else if (msg.type == 0x41) {
			state = TypeConverter.toLong(msg.data[2]);
			err = TypeConverter.toLong(msg.data[0]);
		}

		String err_str = ((err & 0x80) > 0) ? "low" : "ok";
		batt = ((err & 0x80) > 0) ? 5 : 95;

		
		if (state == 0x00)
			stateStr = isDoorWindowSensor ? "closed" : "dry";
		else if (state == 0x64)
			stateStr =  isDoorWindowSensor ? "unknown" : "damp";  // FIXME 
		else if (state == 0xC8)
			stateStr = isDoorWindowSensor ? "open" :  "wet";

		logger.debug("State of " + (isDoorWindowSensor ? "WindowStatus" : "HighWater") + ":" + stateStr);
		logger.debug("State of Battery: " + err_str);
		setAttributeValue(0x0001, stateStr);
		setAttributeValue(0x0002, batt);
		notifyAttributes();
	}

}
