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

public class MotionDetector extends Device {
	public final static Logger logger = LoggerFactory.getLogger(MotionDetector.class);

	private long oldCnt = 0;
	private boolean motionInRun = false;
	private Thread timer = new Thread();
	private int nextTr = 0;

	public MotionDetector(DeviceDescriptor descriptor, MessageHandler handler, String address, String type, String serial) 
			throws HomeMaticException {
		super(descriptor, handler, address, type, serial);
		addAttribute(0x0001, "Motion", ValueType.BOOLEAN);
		addAttribute(0x0002, "Brightness", ValueType.FLOAT);
		addAttribute(0x0003, "BatteryStatus", ValueType.FLOAT);
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
		// long state = Converter.toLong(msg[2]); // Is also brightness
		if ((msg.type == 0x10 || msg.type == 0x02) && msg.data[0] == 0x06 && msg.data[1] == 0x01) {
			long err = TypeConverter.toLong(msg.data[3]);
			String err_str;
			// long brightness = Converter.toLong(msg[2]);

			if (type.equals("004A"))
				logger.debug("SabotageError: " + (((err & 0x0E) > 0) ? "on" : "off"));
			else
				logger.debug("Cover: " + (((err & 0x0E) > 0) ? "open" : "closed"));

			err_str = ((err & 0x80) > 0) ? "low" : "ok";
			float batt = ((err & 0x80) > 0) ? 5 : 95;
			
			logger.debug("Battery: " + err_str);
			setAttributeValue(0x0003, batt);
			notifyAttributes(0x0003);
		}
		else if (msg.type == 0x41) {
			long cnt = TypeConverter.toLong(msg.data[1]);
			long brightn = TypeConverter.toLong(msg.data[2]);
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

			if (cnt != oldCnt) {
				oldCnt = cnt;
				logger.debug("State: motion");
				setAttributeValue(0x0001, true);
				
				logger.debug("MotionCount: " + cnt + " next Trigger: " + nextTr + "s");
				logger.debug("Brightness: " + brightn);
				setAttributeValue(0x0002, brightn);
				notifyAttributes(0x0002, 0x0001);
				
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
									setAttributeValue(0x0001, false);
									notifyAttributes(0x0001);
									logger.debug("Reset State: no motion");
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

}
