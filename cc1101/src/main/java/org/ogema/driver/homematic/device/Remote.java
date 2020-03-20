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
import org.ogema.driver.homematic.data.Value;
import org.ogema.driver.homematic.data.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Remote extends Device {

	private final Logger logger = LoggerFactory.getLogger(Remote.class);
	private long btncnt = 0;
	private byte oldflag = 0x00;
	private int numOfSwitches;

	public Remote(DeviceDescriptor descriptor, MessageHandler handler, String address, String type, String serial) 
			throws HomeMaticException {
		super(descriptor, handler, address, type, serial);
		addAttribute(0x0300, "BatteryStatus", ValueType.FLOAT);
		
		// Get number of button channels
		String[] channels = descriptor.getChannels(type);
		for (String channel : channels) {
			String[] splitChannel = channel.split(":");
			numOfSwitches = Integer.parseInt(splitChannel[2]) - Integer.parseInt(splitChannel[1]) + 1;
			if (splitChannel[0].equals("Sw") || splitChannel[0].equals("Btn")) {
				for (int i = 1; i <= numOfSwitches; i++) {
					addAttribute(i, "ShortPressedButton" + i, ValueType.BOOLEAN);
					addAttribute((i + 0x100), "LongPressedButton" + i, ValueType.BOOLEAN);
				}
			}
		}
	}

	@Override
	public void parseMessage(StatusMessage msg, CommandMessage cmd) {
		byte msgType = msg.type;
		byte contentType = msg.data[0];

		if ((msgType == 0x10 && ((contentType == 0x02) || (contentType == 0x03)))) {
			// Configuration response Message
			parseConfig(msg, cmd);
		}
		else {
			parseValue(msg);
		}
	}

	@Override
	protected void parseValue(StatusMessage msg) {

		if ((msg.type & 0xF0) == 0x40) {
			int button = (msg.data[0] & 0x3F);
			if ((msg.data[0] & 0x40) > 0) {
				if (msg.flag != oldflag) { // long press
					button += 0x100;
					oldflag = msg.flag;
					if ((msg.flag & 0x20) > 0) {
						setAttributeValue(button, false); // Release
						logger.debug("Long Pressed button: " + false);
					}
					else if (msg.data[1] != btncnt) {
						setAttributeValue(button, true); // Press
						logger.debug("Long Pressed button: " + true);
					}
				}
			}
			else if (msg.data[1] != btncnt) { // short press
				boolean state;
				Value value = getAttributeValue(button);
				if (value != null) {
					state = !value.asBoolean();
				}
				else {
					state = true;
				}
				setAttributeValue(button, state); // press
				logger.debug("Short Pressed button value: " + state);
				logger.debug("Short Pressed button count: " + btncnt);
			}
			String error = ((msg.data[0] & 0x80) > 0) ? "low" : "ok";
			float batt = ((msg.data[0] & 0x80) > 0) ? 5 : 95;
			btncnt = msg.data[1];
			
			logger.debug("Battery: " + error);
			setAttributeValue(0x0300, batt);
			notifyAttributes(0x0300, button);
		}
	}
}
