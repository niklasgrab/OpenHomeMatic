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
import org.ogema.driver.homematic.com.message.MessageHandler;
import org.ogema.driver.homematic.com.message.StatusMessage;
import org.ogema.driver.homematic.data.TypeConverter;
import org.ogema.driver.homematic.data.ValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SwitchMeterPlug extends SwitchPlug {
	public final static Logger logger = LoggerFactory.getLogger(SwitchMeterPlug.class);

	public SwitchMeterPlug(DeviceDescriptor descriptor, MessageHandler handler, String id, String type, String serial) 
			throws HomeMaticException {
		super(descriptor, handler, id, type, serial);
		addAttribute(0x0002, "Current", ValueType.FLOAT);
		addAttribute(0x0003, "Voltage", ValueType.FLOAT);
		addAttribute(0x0004, "Power", ValueType.FLOAT);
		addAttribute(0x0005, "Frequency", ValueType.FLOAT);
		addAttribute(0x0006, "Energy", ValueType.FLOAT);
	}

	@Override
	protected void parseValue(StatusMessage msg) {
		String stateStr = "";
		String timeOn;
		
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
			timeOn = ((err & 0x40) > 0) ? "running" : "off";
			
			logger.debug("State: " + stateStr);
			logger.debug("Timed-on: " + timeOn);
			notifyAttributes(0x0001);
		}
		else if (msg.type == 0x5E || msg.type == 0x5F) {
			// The Metering Story
			float energy = ((float) TypeConverter.toLong(msg.data, 0, 3)) / 10;
			float power = ((float) TypeConverter.toLong(msg.data, 3, 3)) / 100;
			float current = ((float) TypeConverter.toLong(msg.data, 6, 2)) / 1;
			float voltage = ((float) TypeConverter.toLong(msg.data, 8, 2)) / 10;
			float frequency = ((float) TypeConverter.toLong(msg.data[10])) / 100 + 50;
			boolean boot = (TypeConverter.toLong(msg.data, 0, 3) & 0x800000) > 0;
			
			logger.debug("Current: " + current + " mA");
			setAttributeValue(0x0002, current);
			
			logger.debug("Voltage: " + voltage + " V");
			setAttributeValue(0x0003, voltage);
			
			logger.debug("Power: " + power + " W");
			setAttributeValue(0x0004, power);
			
			logger.debug("Frequence: " + frequency + " Hz");
			setAttributeValue(0x0005, frequency);
			
			logger.debug("Energy Counter: " + energy + " Wh");
			setAttributeValue(0x0006, energy);
			
			logger.debug("Boot: " + boot);
			notifyAttributes(0x0002, 0x0003, 0x0004, 0x0005, 0x0006);
		}
	}

}
