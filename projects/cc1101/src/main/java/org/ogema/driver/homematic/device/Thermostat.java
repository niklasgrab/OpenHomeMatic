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

public class Thermostat extends Device {
	public final static Logger logger = LoggerFactory.getLogger(Thermostat.class);

	private static final String THERMOSTAT_KEY = "0095";

	public Thermostat(DeviceDescriptor descriptor, MessageHandler handler, String id, String type, String serial) 
			throws HomeMaticException {
		super(descriptor, handler, id, type, serial);
		addCommand(0x01, "TemperatureSetpoint", ValueType.FLOAT);
		addAttribute(0x0001, "TemperatureSetpoint", ValueType.FLOAT);
		addAttribute(0x0002, "Temperature", ValueType.FLOAT);
		addAttribute(0x0003, "ValvePosition", ValueType.FLOAT);
		addAttribute(0x0004, "BatteryVoltage", ValueType.FLOAT);
	}

	public void parseMessage(StatusMessage msg, CommandMessage cmd) {
		byte msgType = msg.type;
		byte contentType = msg.data[0];
		if ((msgType == 0x10 && contentType == 0x0A) || (msgType == 0x02 && contentType == 0x01)) {
			parseValue(msg);
		}
		else if ((msgType == 0x10 && (contentType == 0x02) || (contentType == 0x03))) {
			// Configuration response Message
			parseConfig(msg, cmd);
		}
		else if (msgType == 0x59) { // inform about new value
			// TODO: team msg
		}
		else if (msgType == 0x3F) { // Timestamp request important!
			// TODO: push @ack,$shash,"${mNo}803F$ioId${src}0204$s2000";
		}
	}

	protected void parseValue(StatusMessage msg) {
		if (!type.equals(THERMOSTAT_KEY))
			return;
		byte msgType = msg.type;
		float bat = 0;
		float remoteCurrentTemp = 0;
		long desTemp = 0;
		long valvePos = 0;
		long err = 0;
		String errStr = "";
		long ctrlMode = 0;
		String ctrlModeStr = "";
		
		if (msgType == 0x10) {
			bat = ((float) (TypeConverter.toLong(msg.data[3] & 0x1F))) / 10 + 1.5F;
			remoteCurrentTemp = ((float) (TypeConverter.toLong(msg.data, 1, 2) & 0x3FF)) / 10;
			desTemp = (TypeConverter.toLong(msg.data, 1, 2) >> 10);
			valvePos = TypeConverter.toLong(msg.data[4] & 0x7F);
			err = TypeConverter.toLong(msg.data[3] >> 5);
		}
		else {
			desTemp = TypeConverter.toLong(msg.data, 1, 2);
			err = TypeConverter.toLong(msg.data[3] >> 1);
		}
		float remoteDesiredTemp = (desTemp & 0x3f) / 2;
		setAttributeValue(0x0001, remoteDesiredTemp);
		
		err = err & 0x7;
		ctrlMode = TypeConverter.toLong((msg.data[5] >> 6) & 0x3);
		
		if (msg.length >= 7) { // Messages with Party Mode
			// TODO: Implement Party features
		}

		if ((msg.length >= 6) && (ctrlMode == 3)) { // Msg with Boost
			// TODO: Calculation with Boost Time
		}
		switch (TypeConverter.toInt(err)) {
		case 0:
			errStr = "OK";
			break;
		case 1:
			errStr = "Valve tight";
			break;
		case 2:
			errStr = "Adjust range too large";
			break;
		case 3:
			errStr = "Adjust range too small";
			break;
		case 4:
			errStr = "Communication error";
			break;
		case 5:
			errStr = "Unknown";
			break;
		case 6:
			errStr = "Low Battery";
			break;
		case 7:
			errStr = "Valve error position";
			break;
		}

		switch (TypeConverter.toInt(ctrlMode)) {
		case 0:
			ctrlModeStr = "Auto";
			break;
		case 1:
			ctrlModeStr = "Manual";
			break;
		case 2:
			ctrlModeStr = "Party";
			break;
		case 3:
			ctrlModeStr = "Boost";
			break;
		default:
			ctrlModeStr = Long.toHexString(ctrlMode);
			break;
		}

		logger.debug("Measured Temperature: " + remoteCurrentTemp + " C");
		setAttributeValue(0x0002, remoteCurrentTemp);
		
		logger.debug("Desired Temperature: " + remoteDesiredTemp + " C");
		logger.debug("Battery Voltage: " + bat + " V");
		setAttributeValue(0x0004, bat);
		
		logger.debug("Valve Position: " + valvePos + " %");
		setAttributeValue(0x0003, valvePos / 100);
		
		logger.debug("Error: " + errStr);
		logger.debug("Control Mode: " + ctrlModeStr);
		notifyAttributes();
	}

	@Override
	public void sendCommand(byte identifier, Value value) throws HomeMaticException {
		if (identifier == 0x01) { // desiredTemp
			float localDesiredTemp = value.asFloat();
			localDesiredTemp = (float) (Math.ceil(localDesiredTemp * 2) / 2);
			if (localDesiredTemp > 31.5)
				localDesiredTemp = 31.5f;
			if (localDesiredTemp < 0)
				localDesiredTemp = 0;
			float f = (localDesiredTemp * 2.0f);
			int i = (int) f;
			byte b = (byte) (i & 0x000000FF);
			String bs = TypeConverter.toHexString(b);
			
			// Syntax: Command + Desiredtemp * 2 + Flag + Type
			messageHandler.sendMessage(id, (byte) 0xB0, (byte) 0x11, "8104" + bs);
		}
	}

}
