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


import org.ogema.driver.homematic.manager.asksin.messages.CmdMessage;
import org.ogema.driver.homematic.usbconnection.IUsbConnection;

public class LocalDevice extends org.ogema.driver.homematic.manager.LocalDevice {

	private boolean ignoreExisting;
	private boolean createHandlers = false;

	public LocalDevice(String port, IUsbConnection con) {
		super(port, con);
		createHandlers = true;
		initialize();
	}

	@Override
	protected void initialize() {
		if (!createHandlers) return;
		messageHandler = new AsksinMessageHandler(this);
		inputHandler = new AsksinInputHandler(this);
		inputHandlerThread = new Thread(inputHandler);
		inputHandlerThread.setName("OGEMA-HomeMatic-CC1101-Asksin-inputHandler");
		inputHandlerThread.start();
	}
	
	@Override
	public void sendCmdMessage(org.ogema.driver.homematic.manager.RemoteDevice rd, byte flag, byte type, String data) {
		CmdMessage cmdMessage = new CmdMessage(this, (RemoteDevice)rd, flag, type, data);
		messageHandler.sendMessage(cmdMessage);
	}

	@Override
	public void sendCmdMessage(org.ogema.driver.homematic.manager.RemoteDevice rd, byte flag, byte type, byte[] data) {
		CmdMessage cmdMessage = new CmdMessage(this, (RemoteDevice)rd, flag, type, data);
		messageHandler.sendMessage(cmdMessage);
	}

	@Override
	public void saveDeviceConfig() {
		// Do nothing as local file storage will be handled somewhere else
	}

	public boolean getIsReady() {
		return isReady;
	}

	public boolean isIgnoreExisting() {
		return ignoreExisting;
	}

	public void setIgnoreExisting(boolean ignoreExisting) {
		this.ignoreExisting = ignoreExisting;
		for (org.ogema.driver.homematic.manager.RemoteDevice rm : devices.values()) {
			((RemoteDevice)rm).setIgnore(true);
		}
	}
}
