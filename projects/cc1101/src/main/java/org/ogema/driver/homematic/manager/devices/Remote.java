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
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.Value;
import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.DeviceAttribute;
import org.ogema.driver.homematic.manager.MessageHandler;
import org.ogema.driver.homematic.manager.ValueType;
import org.ogema.driver.homematic.manager.messages.CommandMessage;
import org.ogema.driver.homematic.manager.messages.StatusMessage;

public class Remote extends Device {

	private long btncnt = 0;
	private byte oldflag = 0x00;
	private int numOfSwitches;

	public Remote(DeviceDescriptor descriptor, MessageHandler messageHandler, String address, String deviceKey, 
			String serial, boolean paired) {
		super(descriptor, messageHandler, address, deviceKey, serial, paired);
	}

	@Override
	protected void configureChannels() {
		deviceAttributes.put((short) 0x0300, new DeviceAttribute((short) 0x0300, "batteryStatus", true, true, ValueType.FLOAT));
		// Get number of button channels
		String[] channels = descriptor.getChannels(key);
		for (String channel : channels) {
			String[] splitChannel = channel.split(":");
			numOfSwitches = Integer.parseInt(splitChannel[2]) - Integer.parseInt(splitChannel[1]) + 1;
			if (splitChannel[0].equals("Sw") || splitChannel[0].equals("Btn")) {
				for (int i = 1; i <= numOfSwitches; i++) {
					deviceAttributes.put((short) i, new DeviceAttribute((short) i, "shortPressedButton_" + i, true,
							true, ValueType.BOOLEAN));
					deviceAttributes.put((short) (i + 0x100), new DeviceAttribute((short) (i + 0x100),
							"longPressedButton_" + (i), true, true, ValueType.BOOLEAN));
				}
			}
		}
	}

	@Override
	protected void parseValue(StatusMessage msg) {

		if ((msg.type & 0xF0) == 0x40) {
			final int btn_no = (msg.data[0] & 0x3F);
			if ((msg.data[0] & 0x40) > 0) {
				if (msg.flag != oldflag) { // long press
					oldflag = msg.flag;
					if ((msg.flag & 0x20) > 0) {
						deviceAttributes.get((short) (btn_no + 0x100)).setValue(new BooleanValue(false)); // Release
						System.out.println("Long Pressed button: " + false);
					}
					else if (msg.data[1] != btncnt) {
						deviceAttributes.get((short) (btn_no + 0x100)).setValue(new BooleanValue(true)); // Press
						System.out.println("Long Pressed button: " + true);
					}
				}
			}
			else if (msg.data[1] != btncnt) { // short press
				BooleanValue oldValue = (BooleanValue) deviceAttributes.get((short) btn_no).getValue();
				if (oldValue == null)
					oldValue = new BooleanValue(false);
				deviceAttributes.get((short) btn_no).setValue(new BooleanValue(!oldValue.asBoolean())); // press
				System.out.println("Short Pressed button value: " + !oldValue.asBoolean());
				System.out.println("Short Pressed button count: " + btncnt);
			}
			String err_str = ((msg.data[0] & 0x80) > 0) ? "low" : "ok";
			float batt = ((msg.data[0] & 0x80) > 0) ? 5 : 95;
			System.out.println("Battery: " + err_str);
			deviceAttributes.get((short) 0x0300).setValue(new FloatValue(batt));
			btncnt = msg.data[1];
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

		if ((msgType == 0x10 && ((contentType == 0x02) || (contentType == 0x03)))) {
			// Configuration response Message
			parseConfig(msg, cmd);
		}
		else {
			parseValue(msg);
		}
	}
}
