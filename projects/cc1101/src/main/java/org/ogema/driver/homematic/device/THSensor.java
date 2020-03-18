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

public class THSensor extends Device {
	public final static Logger logger = LoggerFactory.getLogger(THSensor.class);

	public THSensor(DeviceDescriptor descriptor, MessageHandler handler, String id, String type, String serial) 
			throws HomeMaticException {
		super(descriptor, handler, id, type, serial);
		addAttribute(0x0001, "Temperature", ValueType.FLOAT);
		addAttribute(0x0002, "Humidity", ValueType.FLOAT);
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
		long temp = 0;
		long hum = 0;
		long err = 0;
		String errStr = "";
		float batt = 95;

		if (msg.type == 0x70) {
			temp = TypeConverter.toLong(msg.data, 0, 2);
			errStr = ((temp & 0x8000) > 0) ? "low" : "ok";
			batt = ((temp & 0x8000) > 0) ? 5 : 95;
			if ((temp & 0x4000) > 0)
				temp -= 0x8000;

			if (msg.data.length > 2)
				hum = TypeConverter.toLong(msg.data, 2, 1);
		}
		else if (msg.type == 0x53) {
			temp = TypeConverter.toLong(msg.data, 2, 2);
			if ((temp & 0xC00) > 0)
				temp -= 0x10000;
			err = TypeConverter.toLong(msg.data[0]);
			errStr = ((err & 0x80) > 0) ? "low" : "ok";
			batt = ((temp & 0x8000) > 0) ? 5 : 95;
		}

		logger.debug("Temperature: " + ((float) temp) / 10 + " C");
		setAttributeValue(0x0001, temp / 10f);
		if (hum < 100) {
			logger.debug("Humidity: " + hum + "%");
			setAttributeValue(0x0002, hum);
		}
		logger.debug("State of Battery: " + errStr);
		setAttributeValue(0x0003, batt);
		notifyAttributes();
	}

	@Override
	protected String[] getPushConfigData(String channel, String list, String pushConfigs) {
		String[] pushConfigData = null;
		logger.debug("Name of device: " + name);
		//TODO We don't know if there exists normal pairing sensors
//		if (name.equals("HM-WDS30-T-O") || name.equals("HM-WDS40-TH-I-2")) { 
			pushConfigData = new String[1];
			pushConfigData[0] = channel + "0500000000" + list;
//		}
//		else {
//			pushConfigData = super.getPushConfigData(channel, list, pushConfigs);
//		}
		return pushConfigData;
	}
}
