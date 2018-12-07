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

import org.ogema.driver.homematic.HomeMaticException;
import org.ogema.driver.homematic.data.FloatValue;
import org.ogema.driver.homematic.data.Value;
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

public class Thermostat extends Device {
	public final static Logger logger = LoggerFactory.getLogger(Thermostat.class);

	private static final String THERMOSTAT_KEY = "0095";

	public Thermostat(DeviceDescriptor descriptor, MessageHandler messageHandler, String address, String deviceKey, String serial) 
			throws HomeMaticException {
		super(descriptor, messageHandler, address, deviceKey, serial);
	}

	@Override
	protected void configureChannels() {
		deviceCommands.put((byte) 0x01, new DeviceCommand(this, (byte) 0x01, "desiredTemp", true, ValueType.FLOAT));
		deviceAttributes.put((short) 0x0001, new DeviceAttribute((short) 0x0001, "desiredTemp", true, true, ValueType.FLOAT));
		deviceAttributes.put((short) 0x0002, new DeviceAttribute((short) 0x0002, "currentTemp", true, true, ValueType.FLOAT));
		deviceAttributes.put((short) 0x0003, new DeviceAttribute((short) 0x0003, "ValvePosition", true, true, ValueType.FLOAT));
		deviceAttributes.put((short) 0x0004, new DeviceAttribute((short) 0x0004, "batteryVoltage", true, true, ValueType.FLOAT));
	}

	public void parseMessage(StatusMessage msg, CommandMessage cmd, Device device) {
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
		if (!key.equals(THERMOSTAT_KEY))
			return;
		byte msgType = msg.type;
		float bat = 0;
		float remoteCurrentTemp = 0;
		long desTemp = 0;
		long valvePos = 0;
		long err = 0;
		String err_str = "";
		long ctrlMode = 0;
		String ctrlMode_str = "";

		if (msgType == 0x10) {
			bat = ((float) (Converter.toLong(msg.data[3] & 0x1F))) / 10 + 1.5F;
			remoteCurrentTemp = ((float) (Converter.toLong(msg.data, 1, 2) & 0x3FF)) / 10;
			desTemp = (Converter.toLong(msg.data, 1, 2) >> 10);
			valvePos = Converter.toLong(msg.data[4] & 0x7F);
			err = Converter.toLong(msg.data[3] >> 5);
		}
		else {
			desTemp = Converter.toLong(msg.data, 1, 2);
			err = Converter.toLong(msg.data[3] >> 1);
		}
		float remoteDesiredTemp = (desTemp & 0x3f) / 2;
		deviceAttributes.get((short) 0x0001).setValue(new FloatValue(remoteDesiredTemp));
		err = err & 0x7;
		ctrlMode = Converter.toLong((msg.data[5] >> 6) & 0x3);

		if (msg.length >= 7) { // Messages with Party Mode
			// TODO: Implement Party features
		}

		if ((msg.length >= 6) && (ctrlMode == 3)) { // Msg with Boost
			// TODO: Calculation with Boost Time
		}
		switch (Converter.toInt(err)) {
		case 0:
			err_str = "OK";
			break;
		case 1:
			err_str = "Valve tight";
			break;
		case 2:
			err_str = "Adjust range too large";
			break;
		case 3:
			err_str = "Adjust range too small";
			break;
		case 4:
			err_str = "Communication error";
			break;
		case 5:
			err_str = "Unknown";
			break;
		case 6:
			err_str = "Low Battery";
			break;
		case 7:
			err_str = "Valve error position";
			break;
		}

		switch (Converter.toInt(ctrlMode)) {
		case 0:
			ctrlMode_str = "Auto";
			break;
		case 1:
			ctrlMode_str = "Manual";
			break;
		case 2:
			ctrlMode_str = "Party";
			break;
		case 3:
			ctrlMode_str = "Boost";
			break;
		default:
			ctrlMode_str = Long.toHexString(ctrlMode);
			break;
		}

		logger.debug("Measured Temperature: " + remoteCurrentTemp + " C");
		deviceAttributes.get((short) 0x0002).setValue(new FloatValue(remoteCurrentTemp));
		logger.debug("Desired Temperature: " + remoteDesiredTemp + " C");
		logger.debug("Battery Voltage: " + bat + " V");
		deviceAttributes.get((short) 0x0004).setValue(new FloatValue(bat));
		logger.debug("Valve Position: " + valvePos + " %");
		deviceAttributes.get((short) 0x0003).setValue(new FloatValue(valvePos / 100));
		logger.debug("Error: " + err_str);
		logger.debug("Control Mode: " + ctrlMode_str);
	}

	@Override
	public void channelChanged(byte identifier, Value value) throws HomeMaticException {
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
			String bs = Converter.toHexString(b);
			// Syntax: Commando + Desiredtemp * 2 + Flag + Type
			messageHandler.sendMessage(address, (byte) 0xB0, (byte) 0x11, "8104" + bs);
		}
	}
}
