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
import org.ogema.driver.homematic.data.Value;
import org.ogema.driver.homematic.data.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwitchPlug extends Device {
	public final static Logger logger = LoggerFactory.getLogger(SwitchPlug.class);

	public SwitchPlug(DeviceDescriptor descriptor, MessageHandler handler, String id, String type, String serial) 
			throws HomeMaticException {
		super(descriptor, handler, id, type, serial);
		addCommand(0x01, "State", ValueType.BOOLEAN);
		addAttribute(0x0001, "State", ValueType.BOOLEAN);
	}

	@Override
	public void activate() throws HomeMaticException {
		// Get state
		messageHandler.sendMessage(id, (byte) 0xA0, (byte) 0x01, "010E");
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
		String stateStr = "";
		String timedOn;

		if ((msg.type == 0x10 && msg.data[0] == 0x06) || (msg.type == 0x02 && msg.data[0] == 0x01)) {
			// The whole button story
			long state = TypeConverter.toLong(msg.data[2]);
			if (state == 0x00) {
				stateStr = "off";
				setAttributeValue(0x0001, false);
			}
			else if (state == 0xC8) {
				stateStr = "on";
				setAttributeValue(0x0001, true);
			}
			long err = TypeConverter.toLong(msg.data[3]);
			timedOn = ((err & 0x40) > 0) ? "running" : "off";
			
			logger.debug("State: " + stateStr);
			logger.debug("Timed-on: " + timedOn);
			notifyAttributes();
		}
		else if (msg.type == 0x5E || msg.type == 0x5F) {
			boolean boot = (TypeConverter.toLong(msg.data, 0, 3) & 0x800000) > 0;
			logger.debug("Boot: " + boot);
		}
	}

	@Override
	public void sendCommand(byte id, Value value) throws HomeMaticException {
		if (id == 0x01) {
			messageHandler.sendMessage(this.id, (byte) 0xA0, (byte) 0x11, "0201" + (value.asBoolean() ? "C8" : "00") + "0000");
			
			// Get state
			messageHandler.sendMessage(this.id, (byte) 0xA0, (byte) 0x01, "010E");
		}
	}
}
