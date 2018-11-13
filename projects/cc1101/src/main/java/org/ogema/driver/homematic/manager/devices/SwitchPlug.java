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

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.Value;
import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.DeviceAttribute;
import org.ogema.driver.homematic.manager.DeviceCommand;
import org.ogema.driver.homematic.manager.MessageHandler;
import org.ogema.driver.homematic.manager.ValueType;
import org.ogema.driver.homematic.manager.messages.CommandMessage;
import org.ogema.driver.homematic.manager.messages.StatusMessage;
import org.ogema.driver.homematic.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwitchPlug extends Device {
	public final static Logger logger = LoggerFactory.getLogger(SwitchPlug.class);

	private BooleanValue isOn;

	public SwitchPlug(DeviceDescriptor descriptor, MessageHandler messageHandler, String address, String deviceKey, String serial) {
		super(descriptor, messageHandler, address, deviceKey, serial);
	}

	@Override
	protected void configureChannels() {
		deviceCommands.put((byte) 0x01, new DeviceCommand(this, (byte) 0x01, "onOff", true, ValueType.BOOLEAN));
		deviceAttributes.put((short) 0x0001, new DeviceAttribute((short) 0x0001, "isOn", true, true, ValueType.BOOLEAN));
		//deviceAttributes.put((short) 0x0002, new DeviceAttribute((short) 0x0002, "iRes", true, true));
		//deviceAttributes.put((short) 0x0003, new DeviceAttribute((short) 0x0003, "vRes", true, true));
		//deviceAttributes.put((short) 0x0004, new DeviceAttribute((short) 0x0004, "pRes", true, true));
		//deviceAttributes.put((short) 0x0005, new DeviceAttribute((short) 0x0005, "fRes", true, true));
		//deviceAttributes.put((short) 0x0006, new DeviceAttribute((short) 0x0006, "eRes", true, true));

		// Get state
		messageHandler.sendMessage(address, (byte) 0xA0, (byte) 0x01, "010E");
	}

	@Override
	protected void parseValue(StatusMessage msg) {

		String state_str = "";
		String timedon;

		if ((msg.type == 0x10 && msg.data[0] == 0x06) || (msg.type == 0x02 && msg.data[0] == 0x01)) {
			// The whole button story
			long state = Converter.toLong(msg.data[2]);
			if (state == 0x00) {
				state_str = "off";
				isOn = new BooleanValue(false);
				deviceAttributes.get((short) 0x0001).setValue(isOn);
			}
			else if (state == 0xC8) {
				state_str = "on";
				isOn = new BooleanValue(true);
				deviceAttributes.get((short) 0x0001).setValue(isOn);
			}

			long err = Converter.toLong(msg.data[3]);
			timedon = ((err & 0x40) > 0) ? "running" : "off";

			logger.debug("State: " + state_str);
			logger.debug("Timed-on: " + timedon);
		}
		else if (msg.type == 0x5E || msg.type == 0x5F) {
			// The Metering Story
			//float eCnt = ((float) Converter.toLong(msg.msg_data, 0, 3)) / 10;
			//float power = ((float) Converter.toLong(msg.msg_data, 3, 3)) / 100;
			//float current = ((float) Converter.toLong(msg.msg_data, 6, 2)) / 1;
			//float voltage = ((float) Converter.toLong(msg.msg_data, 8, 2)) / 10;
			//float frequence = ((float) Converter.toLong(msg.msg_data[10])) / 100 + 50;
			boolean boot = (Converter.toLong(msg.data, 0, 3) & 0x800000) > 0;

			//HMDriver.logger.debug("Energy Counter: " + eCnt + " Wh");
			//deviceAttributes.get((short) 0x0006).setValue(new FloatValue(eCnt));
			//HMDriver.logger.debug("Power: " + power + " W");
			//deviceAttributes.get((short) 0x0004).setValue(new FloatValue(power));
			//HMDriver.logger.debug("Current: " + current + " mA");
			//deviceAttributes.get((short) 0x0002).setValue(new FloatValue(current));
			//HMDriver.logger.debug("Voltage: " + voltage + " V");
			//deviceAttributes.get((short) 0x0003).setValue(new FloatValue(voltage));
			//HMDriver.logger.debug("Frequence: " + frequence + " Hz");
			//deviceAttributes.get((short) 0x0005).setValue(new FloatValue(frequence));
			logger.debug("Boot: " + boot);
		}
	}

	@Override
	public void channelChanged(byte identifier, Value value) {
		if (identifier == 0x01) {
			BooleanValue v = (BooleanValue)value;
			messageHandler.sendMessage(address, (byte) 0xA0, (byte) 0x11, "0201" + ((v.asBoolean()) ? "C8" : "00") + "0000");
			
			// Get state
			messageHandler.sendMessage(address, (byte) 0xA0, (byte) 0x01, "010E");
		}
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
