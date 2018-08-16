/*
 * Copyright 2016-18 ISC Konstanz
 *
 * This file is part of OpenHomeMatic.
 * For more information visit https://github.com/isc-konstanz/OpenHomeMatic.
 *
 * OpenHomeMatic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenHomeMatic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenHomeMatic.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.ogema.driver.homematic.manager.asksin;

import java.util.Arrays;

import org.ogema.driver.homematic.connection.serial.CulConnection;
import org.ogema.driver.homematic.manager.InputHandler;
import org.ogema.driver.homematic.manager.StatusMessage;
import org.ogema.driver.homematic.manager.RemoteDevice.InitStates;
import org.ogema.driver.homematic.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AsksinInputHandler extends InputHandler implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(AsksinInputHandler.class);

	private static final String OWNER_ID_KEY = "onwerId";
	private static final String OWNER_ID_DEFAULT = "F11034";

	public AsksinInputHandler(LocalDevice localDevice) {
		super(localDevice);
	}
	
	public void handleMessage(byte[] tempArray) {
		logger.debug("message type: " + (char) tempArray[0]);
		switch (tempArray[0]) {
		case 'V':
			if (!localDeviceInited)
				parseSerialAdapterMsg(tempArray);
			break;
		case 'A':
		case 'a':
			if (tempArray[0] == 'A') {
				logger.debug("A message: " + Converter.toHexString(tempArray));
			}
			else {
				logger.debug("A message: " + Converter.dumpHexString(tempArray));
			}
			StatusMessage emsg = new AsksinStatusMessage(tempArray);
			if (emsg.msg_type == 0x00 & localDevice.getPairing() != null) { // if pairing
				logger.debug("Pairing response received");
				RemoteDevice temp_device = new RemoteDevice((LocalDevice)localDevice, emsg);
				if (localDevice.getPairing().equals("0000000000")
						| localDevice.getPairing().equals(temp_device.getSerial())) {

					org.ogema.driver.homematic.manager.RemoteDevice found_device = 
							localDevice.getDevices().get(temp_device.getAddress());
					if (found_device == null) {
						localDevice.getDevices().put(temp_device.getAddress(), temp_device);
						temp_device.init();
					}
					else {
						if (!((LocalDevice)localDevice).isIgnoreExisting()) {
							((RemoteDevice)found_device).setIgnore(false);
						}
						if (found_device.getInitState().equals(InitStates.UNKNOWN)) {
							temp_device = (RemoteDevice)localDevice.getDevices().get(found_device.getAddress());
							temp_device.init();
						}
						else if (found_device.getInitState().equals(InitStates.PAIRED)) {
							temp_device = (RemoteDevice)localDevice.getDevices().get(found_device.getAddress());
							temp_device.initWithoutAddMandatoryChannels();
						}
					}
				}
			}
			else {
				if (localDevice.getOwnerid().equals(emsg.destination) || emsg.destination.equals("000000")
						|| emsg.partyMode) {
					if (localDevice.getDevices().containsKey(emsg.source)) {
						// 000000 = broadcast
						((AsksinMessageHandler)messageHandler).messageReceived(emsg);
					}
					else {
						logger.debug("Unpaired Homematic device detected: " + emsg.source);
					}
				}
			}
			lastMsg = emsg;
			break;
		default:
			if (CulConnection.BINARY_MODE) {
				logger.debug("Unknown message: " + Converter.toHexString(tempArray));
			}
			else {
				logger.debug("Unknown message: " + Converter.dumpHexString(tempArray));
			}
		}
		
	}
	
	private void parseSerialAdapterMsg(byte[] data) {
		// remove \r\n
		data = Arrays.copyOfRange(data, 0, 13);
		
		localDevice.setName("SerialLocalDevice");
		localDevice.setFirmware(new String(data));
		localDevice.setSerial("");
		
		// Used in here, HMRemoteDevice and HMCmdMessage
		String ownerid = System.getProperty(OWNER_ID_KEY, OWNER_ID_DEFAULT);
		localDevice.setOwnerid(ownerid);
		// Used in HMCmdMessage only
		
		localDevice.setUptime(0);
		localDeviceInited = true;
	}	
}
