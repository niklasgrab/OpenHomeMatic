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

public class SmokeSensor extends Device {
	private final Logger logger = LoggerFactory.getLogger(SmokeSensor.class);

	public SmokeSensor(DeviceDescriptor descriptor, MessageHandler handler, String id, String type, String serial) 
			throws HomeMaticException {
		super(descriptor, handler, id, type, serial);
		addAttribute(0x0001, "Temperature", ValueType.BOOLEAN);
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
		if (msg.type == 0x41) {
			long status = TypeConverter.toLong(msg.data[2]);
			long err = TypeConverter.toLong(msg.data[0]);

			String error = ((err & 0x80) > 0) ? "low" : "ok";
			float batt = ((err & 0x80) > 0) ? 5 : 95;
			logger.debug("State of Battery: " + error);
			setAttributeValue(0x0002, batt);

			if (status > 1) {
				logger.debug("Smoke Alert: true");
				setAttributeValue(0x0001, true);
			}
			else {
				logger.debug("Smoke Alert: false");
				setAttributeValue(0x0001, false);
			}
			notifyAttributes();
		}
	}

}
