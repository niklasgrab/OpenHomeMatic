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


import org.ogema.driver.homematic.Activator;
import org.ogema.driver.homematic.connection.ProtocolType;
import org.ogema.driver.homematic.manager.InputHandler;
import org.ogema.driver.homematic.manager.MessageHandler;
import org.ogema.driver.homematic.manager.asksin.messages.CmdMessage;
import org.ogema.driver.homematic.usbconnection.IUsbConnection;

/**
 * 
 * @author Godwin Burkhardt
 * 
 */
public class LocalDevice extends org.ogema.driver.homematic.manager.LocalDevice {

	private FileStorage fileStorage;
	private boolean ignoreExisting;

	private ProtocolType protocolType = ProtocolType.BYTE;

	public LocalDevice(String port, IUsbConnection con, ProtocolType type) {
		super(port, con);
		this.protocolType = type;
		initialize();
	}

	@Override
	protected void initialize() {
		if (protocolType == null) return;
		if (protocolType.equals(ProtocolType.BYTE)) {
			messageHandler = new MessageHandler(this);
			inputHandler = new InputHandler(this);
		}
		else {
			messageHandler = new AsksinMessageHandler(this);
			inputHandler = new AsksinInputHandler(this);
		}
		inputHandlerThread = new Thread(inputHandler);
		inputHandlerThread.setName("homematic-lld-inputHandler");
		inputHandlerThread.start();
		final LocalDevice loc = this;

		Thread loadDevicesThread = new Thread() {
			@Override
			public void run() {
				while (!isReady && Activator.bundleIsRunning) {
					try {
						Thread.sleep(500);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
				fileStorage = new FileStorage(loc);
			}
		};
		loadDevicesThread.setName("homematic-ll-loadDevices");
		loadDevicesThread.start();
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

	public FileStorage getHMFileStorage() {
		return fileStorage;
	}

	@Override
	public void saveDeviceConfig() {
		if (isReady)
			fileStorage.saveDeviceConfig();
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

	public ProtocolType getProtocolType() {
		return protocolType;
	}
}
