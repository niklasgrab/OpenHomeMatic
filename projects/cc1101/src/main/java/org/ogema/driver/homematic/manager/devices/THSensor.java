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

import org.ogema.driver.homematic.HomeMaticConnectionException;
import org.ogema.driver.homematic.data.FloatValue;
import org.ogema.driver.homematic.data.Value;
import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.DeviceAttribute;
import org.ogema.driver.homematic.manager.MessageHandler;
import org.ogema.driver.homematic.manager.ValueType;
import org.ogema.driver.homematic.manager.messages.CommandMessage;
import org.ogema.driver.homematic.manager.messages.StatusMessage;
import org.ogema.driver.homematic.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class THSensor extends Device {
	public final static Logger logger = LoggerFactory.getLogger(THSensor.class);

	private FloatValue temperature;
	private FloatValue humidity;
	private FloatValue batteryStatus;

	public THSensor(DeviceDescriptor descriptor, MessageHandler messageHandler, String address, String deviceKey, String serial) 
			throws HomeMaticConnectionException {
		super(descriptor, messageHandler, address, deviceKey, serial);
	}

	@Override
	protected String[] getPushConfigData(String channel, String list, String pushConfigs) {
		String[] pushConfigData = null;
		logger.debug("name of device: " + name);
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
	
	@Override
	protected void configureChannels() {
		deviceAttributes.put((short) 0x0001, new DeviceAttribute((short) 0x0001, "Temperature", true, true, ValueType.FLOAT));
		deviceAttributes.put((short) 0x0002, new DeviceAttribute((short) 0x0002, "Humidity", true, true, ValueType.FLOAT));
		deviceAttributes.put((short) 0x0003, new DeviceAttribute((short) 0x0003, "BatteryStatus", true, true, ValueType.FLOAT));
	}

	@Override
	protected void parseValue(StatusMessage msg) {
		long temp = 0;
		long hum = 0;
		long err = 0;
		String err_str = "";
		float batt = 95;

		if (msg.type == 0x70) {
			temp = Converter.toLong(msg.data, 0, 2);
			err_str = ((temp & 0x8000) > 0) ? "low" : "ok";
			batt = ((temp & 0x8000) > 0) ? 5 : 95;
			if ((temp & 0x4000) > 0)
				temp -= 0x8000;

			if (msg.data.length > 2)
				hum = Converter.toLong(msg.data, 2, 1);
		}
		else if (msg.type == 0x53) {
			temp = Converter.toLong(msg.data, 2, 2);
			if ((temp & 0xC00) > 0)
				temp -= 0x10000;
			err = Converter.toLong(msg.data[0]);
			err_str = ((err & 0x80) > 0) ? "low" : "ok";
			batt = ((temp & 0x8000) > 0) ? 5 : 95;
		}

		logger.debug("Temperatur: " + ((float) temp) / 10 + " C");
		logger.debug("State of Battery: " + err_str);
		temperature = new FloatValue(temp / 10f);
		if (hum < 100) {
			logger.debug("Humidity: " + hum + "%");
			humidity = new FloatValue(hum);
		}
		batteryStatus = new FloatValue(batt);
		deviceAttributes.get((short) 0x0001).setValue(temperature);
		deviceAttributes.get((short) 0x0002).setValue(humidity);
		deviceAttributes.get((short) 0x0003).setValue(batteryStatus);
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
		else {
			parseValue(msg);
		}
	}
}
