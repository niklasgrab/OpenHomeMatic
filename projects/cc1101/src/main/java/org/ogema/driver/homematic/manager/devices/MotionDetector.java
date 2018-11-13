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
import org.ogema.driver.homematic.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MotionDetector extends Device {
	public final static Logger logger = LoggerFactory.getLogger(MotionDetector.class);

	private long old_cnt = 0;
	private boolean motionInRun = false;
	private Thread timer = new Thread();
	private int nextTr = 0;

	public MotionDetector(DeviceDescriptor descriptor, MessageHandler messageHandler, String address, String deviceKey, String serial) {
		super(descriptor, messageHandler, address, deviceKey, serial);
	}

	@Override
	protected void configureChannels() {
		deviceAttributes.put((short) 0x0001, new DeviceAttribute((short) 0x0001, "motion", true, true, ValueType.BOOLEAN));
		deviceAttributes.put((short) 0x0002, new DeviceAttribute((short) 0x0002, "brightness", true, true, ValueType.FLOAT));
		deviceAttributes.put((short) 0x0003, new DeviceAttribute((short) 0x0003, "batteryStatus", true, true, ValueType.FLOAT));
	}

	@Override
	protected void parseValue(StatusMessage msg) {

		// long state = Converter.toLong(msg[2]); // Is also brightness
		if ((msg.type == 0x10 || msg.type == 0x02) && msg.data[0] == 0x06 && msg.data[1] == 0x01) {
			long err = Converter.toLong(msg.data[3]);
			String err_str;
			// long brightness = Converter.toLong(msg[2]);

			if (key.equals("004A"))
				logger.debug("SabotageError: " + (((err & 0x0E) > 0) ? "on" : "off"));
			else
				logger.debug("Cover: " + (((err & 0x0E) > 0) ? "open" : "closed"));

			err_str = ((err & 0x80) > 0) ? "low" : "ok";
			float batt = ((err & 0x80) > 0) ? 5 : 95;
			logger.debug("Battery: " + err_str);
			deviceAttributes.get((short) 0x0003).setValue(new FloatValue(batt));
		}
		else if (msg.type == 0x41) {
			long cnt = Converter.toLong(msg.data[1]);
			long brightn = Converter.toLong(msg.data[2]);
			switch (msg.data[3]) {
			case (byte) 0x40:
				nextTr = 15;
				break;
			case (byte) 0x50:
				nextTr = 30;
				break;
			case (byte) 0x60:
				nextTr = 60;
				break;
			case (byte) 0x70:
				nextTr = 120;
				break;
			case (byte) 0x80:
				nextTr = 240;
				break;
			}

			if (cnt != old_cnt) {
				old_cnt = cnt;
				logger.info("State: motion");
				deviceAttributes.get((short) 0x0001).setValue(new BooleanValue(true));
				logger.info("MotionCount: " + cnt + " next Trigger: " + nextTr + "s");
				logger.info("Brightness: " + brightn);
				deviceAttributes.get((short) 0x0002).setValue(new FloatValue(brightn));
				if (timer.isAlive()) {
					motionInRun = true;
				}
				else {
					timer = new Thread() {
						@Override
						public void run() {
							boolean repeat = true;
							while (repeat) {
								try {
									Thread.sleep((nextTr + 1) * 1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								if (motionInRun) {
									motionInRun = false;
								}
								else {
									repeat = false;
									deviceAttributes.get((short) 0x0001).setValue(new BooleanValue(false));
									logger.info("reset State: no motion");
								}
							}
						}
					};
					timer.setName("homematic-ll-timer");
					timer.start();

				}
			}
		}
		else if (msg.type == 0x70 && msg.data[0] == 0x7F) {
			// TODO: NYI
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
		else
			parseValue(msg);

	}
}
