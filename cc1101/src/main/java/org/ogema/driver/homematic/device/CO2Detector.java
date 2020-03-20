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

public class CO2Detector extends Device {
	private final Logger logger = LoggerFactory.getLogger(CO2Detector.class);

	public CO2Detector(DeviceDescriptor descriptor, MessageHandler handler, String id, String type, String serial) 
			throws HomeMaticException {
		super(descriptor, handler, id, type, serial);
		addAttribute(0x0001, "Concentration", ValueType.FLOAT);
	}

	@Override
	public void parseMessage(StatusMessage msg, CommandMessage cmd) {
		byte msgType = msg.type;
		byte contentType = msg.data[0];

		if (getType().equals("0056") || getType().equals("009F")) {
			if ((msg.type == 0x02 && msg.data[0] == 0x01) || (msg.type == 0x10 && msg.data[0] == 0x06)
					|| (msg.type == 0x41)) {
				parseValue(msg);
			}
			else if ((msgType == 0x10 && (contentType == 0x02) || (contentType == 0x03))) {
				// Configuration response Message
				parseConfig(msg, cmd);
			}
		}
		// else if (msg.msg_type == 0x10 && msg.msg_data[0] == 0x06) {
		// // long err = Converter.toLong(msg[3]);
		// state = Converter.toLong(msg.msg_data[2]);
		// String state_str = (state > 2) ? "off" : "smoke-Alarm";
		//
		// logger.debug("Level: " + state);
		// deviceAttributes.get((short) 0x0001).setValue(new FloatValue(state));
		// // String err_str = ((err & 0x80) > 0) ? "low" : "ok";
		// logger.debug("State: " + state_str);
		// }
	}

	@Override
	protected void parseValue(StatusMessage msg) {
		long state = TypeConverter.toLong(msg.data[2]);
		
		if (type.equals("009F"))
			logger.debug("Level: " + state);
		
		setAttributeValue(0x0001, state);
		notifyAttributes();
		logger.debug("CO2 concentration: " + state);
	}

}
