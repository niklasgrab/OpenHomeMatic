/*
 * Copyright 2016-17 ISC Konstanz
 *
 * This file is part of OpenSkeleton.
 * For more information visit https://github.com/isc-konstanz/OpenSkeleton.
 *
 * OpenSkeleton is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenSkeleton is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenSkeleton.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.openmuc.framework.driver.homematic;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.driver.homematic.Activator;
import org.ogema.driver.homematic.connection.LocalConnection;
import org.ogema.driver.homematic.connection.LocalSerialConnection;
import org.ogema.driver.homematic.connection.LocalSerialRaspiPinConnection;
import org.ogema.driver.homematic.connection.LocalUsbConnection;
import org.ogema.driver.homematic.manager.RemoteDevice.InitStates;
import org.ogema.driver.homematic.manager.SubDevice;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;
import org.ogema.driver.homematic.manager.asksin.RemoteDevice;
import org.ogema.driver.homematic.manager.devices.PowerMeter;
import org.ogema.driver.homematic.manager.devices.SwitchPlug;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.homematic.HomeMaticConnection.HomeMaticConnectionCallbacks;
import org.openmuc.framework.driver.homematic.options.HomeMaticDevicePreferences;
import org.openmuc.framework.driver.homematic.options.HomeMaticDeviceScanPreferences;
import org.openmuc.framework.driver.homematic.options.HomeMaticDriverInfo;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * @author Godwin Burkhardt
 * 
 */
@Component
public class HomeMaticDriver implements DriverService, HomeMaticConnectionCallbacks {

	private final static Logger logger = LoggerFactory.getLogger(HomeMaticDriver.class);

	public final static HomeMaticDriverInfo DRIVER_INFO = HomeMaticDriverInfo.getInfo();

	public static final String CONNECTION_TYPE_KEY = "org.openmuc.framework.driver.homematic.connectionType";
	public static final String CONNECTION_TYPE_SERIAL_USB = "serialUSB";
	public static final String CONNECTION_TYPE_SERIAL_RASPIPIN = "serialRaspiPin";
	private static int SLEEP_TIME = 1000;
	private static int CONNECTION_TRY_TIMEOUT = 41; // in Seconds

    private volatile boolean isDeviceScanInterrupted = false;

	private final Map<String, LocalConnection> localConnectionsMap; // <interfaceId, connection>
	private final Object connectionLock = new Object();
	private final Map<String, HomeMaticConnection> connectionsMap;
	private final String portname;


	public HomeMaticDriver() {
		localConnectionsMap = new HashMap<String, LocalConnection>();
		connectionsMap = new HashMap<String, HomeMaticConnection>();
		
//		portname = System.getProperty(CONNECTION_TYPE_KEY, USB_CONNECTION_TYPE_SERIAL);
		portname = System.getProperty(CONNECTION_TYPE_KEY, CONNECTION_TYPE_SERIAL_RASPIPIN);
		Activator.bundleIsRunning = true;
		
		establishConnection();
	}
	
	@Override
	public DriverInfo getInfo() {
		return DRIVER_INFO;
	}

	volatile Thread scanForDevicesThread = null;

	@Override
	public void scanForDevices(final String settingsStr, final DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

		listener.scanProgressUpdate(0);
		
		Boolean ignore = null;
		try {
			HomeMaticDeviceScanPreferences preferences = DRIVER_INFO.getDeviceScanPreferences(settingsStr);
			ignore = preferences.getIgnoreExiting();
		} catch (ArgumentSyntaxException e) {
			logger.debug("Can't parse Device Scan Settings: " + e.getMessage());
		}
		
		LocalConnection localCon = findConnection(portname);			

		boolean ignoreExisting = true;
		if (ignore != null) {
			ignoreExisting = ignore;
		}
		
		if (localCon != null) {
			LocalDevice localDevice = localCon.getLocalDevice();
			localDevice.setIgnoreExisting(ignoreExisting);
			localDevice.setPairing("0000000000");
			logger.info("enabled Pairing for 100 seconds");

			for (int i = 0; i < 100; i++) {
                if (isDeviceScanInterrupted) {
                    break;
                }
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (InterruptedException e) {
					logger.error("Severe Error: " + e.getMessage());
					 e.printStackTrace();
				}
				
				for (org.ogema.driver.homematic.manager.RemoteDevice rm: localDevice.getDevices().values()) {
					if (!((RemoteDevice)rm).isIgnore() && ((RemoteDevice)rm).getInitState() == InitStates.PAIRED) {
						String type = rm.getDeviceType();
						String description = localDevice.getDeviceDescriptor().getName(type) + ":" + 
								localDevice.getDeviceDescriptor().getSubType(type);
						DeviceScanInfo info = new DeviceScanInfo(rm.getAddress(), "deviceType=" + type, description);
						listener.deviceFound(info);
					}
				}
		
				listener.scanProgressUpdate(i);
			}
			localDevice.setPairing(null);
			logger.info("Pairing disabled.");
		}
	}

	@Override
	public void interruptDeviceScan() throws UnsupportedOperationException {
        isDeviceScanInterrupted = true;
	}

	@Override
	public Connection connect(String deviceAddressStr, String settingsStr) throws ArgumentSyntaxException, ConnectionException {
		
		logger.debug("Connect HomeMatic device: ", deviceAddressStr);
		
		if (deviceAddressStr.length() != 6) {
			throw new ArgumentSyntaxException("Device Address has wrong length. The length has to be 6 Characters.");
		}
		
		HomeMaticConnection connection = connectionsMap.get(deviceAddressStr);
		
		if (connection == null) {
			LocalConnection localCon = findConnection(portname);			
			connection = new HomeMaticConnection(deviceAddressStr, this);
			connectionsMap.put(deviceAddressStr, connection);
			LocalDevice localDevice = localCon.getLocalDevice();
			connection.setLocalDevice(localDevice);
			if (localDevice.getDevices().get(deviceAddressStr) == null) {
				HomeMaticDevicePreferences preferences = DRIVER_INFO.getDevicePreferences(settingsStr);
				new RemoteDevice(localDevice, deviceAddressStr, 
						preferences.getType(), null); 
				RemoteDevice remoteDevice = (RemoteDevice) localDevice.getDevices().get(deviceAddressStr);
				SubDevice subDevice = remoteDevice.getSubDevice();
				
				if (subDevice instanceof PowerMeter || subDevice instanceof SwitchPlug) {
					BooleanValue defaultState = new BooleanValue(preferences.getDefaultState());
					if (! defaultState.equals(subDevice.deviceAttributes.get((short) 0x0001).getValue())) {
						subDevice.channelChanged((byte) 0x01, defaultState);
						if (! isRemoteDeviceConnected(remoteDevice, defaultState, deviceAddressStr)) {
							connection.close();
							throw new ConnectionException("Device is not paired");
						}
					}
					else if (! isRemoteDeviceConnected(remoteDevice, null, deviceAddressStr)) {
						connection.close();
						throw new ConnectionException("Device is not paired, because no value is sent");
					}
				}
			}
		}

		return connection;

	}
	
	private boolean isRemoteDeviceConnected(RemoteDevice remoteDevice, BooleanValue defaultState, 
			String deviceAddressStr) {
		int connectionTime = 0;
		SubDevice subDevice = remoteDevice.getSubDevice();
		boolean retry = true;
		while (true) {
			try  {
				if (defaultState == null) {
					if (subDevice.deviceAttributes.get((short) 0x0001).getValue() != null) {
						logger.debug("Device " + deviceAddressStr + " is connected, Value is sent");
						return true;
					}	
				}

				Thread.sleep(1000);

				if (defaultState != null) {
					if (defaultState.equals(subDevice.deviceAttributes.get((short) 0x0001).getValue())) {
						logger.debug("Device " + deviceAddressStr + " is connected");
						return true;
					}
					else if (subDevice.deviceAttributes.get((short) 0x0001).getValue() != null) {
						if (retry) {
							remoteDevice.getMsgNum();
							remoteDevice.getMsgNum();
							subDevice.channelChanged((byte) 0x01, defaultState);
							retry = false;
						}
						else {
							logger.debug("Device " + deviceAddressStr + " is not connected (wrong state)");					
							return false;
						}
					}
				}
				connectionTime++;
				if (connectionTime >= CONNECTION_TRY_TIMEOUT) {
					logger.debug("Device " + deviceAddressStr + " is not connected (timedout)");					
					return false;
				}
			} catch (InterruptedException e) {
				break;
			}
		}
		logger.debug("Device " + deviceAddressStr + " is not connected(interrupted)");					
		return false;
	}
	
	private void establishConnection() {
		final String parameter = "HMUSB";
		LocalConnection localCon = localConnectionsMap.get(portname);
		if (localCon == null) {
			try {
				if (portname.equals(CONNECTION_TYPE_SERIAL_RASPIPIN)) {
					localCon = new LocalSerialRaspiPinConnection(connectionLock, portname, parameter);
				}
				else if (portname.equals(CONNECTION_TYPE_SERIAL_USB)) {
					localCon = new LocalSerialConnection(connectionLock, portname, parameter);
				}
				else {
					localCon = new LocalUsbConnection(connectionLock, portname, parameter);
				}
				addConnection(localCon);
			}
			catch (Exception ex) {
				logger.error("Severe Error: " + ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	protected void addConnection(LocalConnection con) {
		localConnectionsMap.put(con.getInterfaceId(), con);
	}

	protected LocalConnection findConnection(String interfaceId) {
		LocalConnection localCon = localConnectionsMap.get(interfaceId);
		synchronized (connectionLock) {
			while (!localCon.hasConnection()) {
				try {
					connectionLock.wait();
				} catch (InterruptedException ex) { // interrupt is used to terminate the thread
				}
			}
		}
		return localCon;
	}

	@Override
	public void onDisconnect(String deviceAddress) {
		connectionsMap.remove(deviceAddress);
	}

}
