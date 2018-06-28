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
package org.openmuc.framework.driver.homematic;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.driver.homematic.connection.LocalConnection;
import org.ogema.driver.homematic.connection.LocalCulConnection;
import org.ogema.driver.homematic.connection.LocalSccConnection;
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
import org.openmuc.framework.config.DriverInfoFactory;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.homematic.HomeMaticConnection.HomeMaticConnectionCallbacks;
import org.openmuc.framework.driver.homematic.settings.DeviceScanSettings;
import org.openmuc.framework.driver.homematic.settings.DeviceSettings;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class HomeMaticDriver implements DriverService, HomeMaticConnectionCallbacks {
	private final static Logger logger = LoggerFactory.getLogger(HomeMaticDriver.class);

	private final DriverInfo info = DriverInfoFactory.getPreferences(HomeMaticDriver.class);

	public static final String CONNECTION_TYPE_KEY = "org.openmuc.framework.driver.homematic.type";
	public static final String CONNECTION_TYPE_CUL = "CUL";
	public static final String CONNECTION_TYPE_SCC = "SCC";

	private static int SLEEP_TIME = 1000;
	private static int CONNECT_TIMEOUT = 41; // in Seconds

    private volatile boolean isDeviceScanInterrupted = false;

	private final Map<String, LocalConnection> localConnectionsMap; // <interfaceId, connection>
	private final Map<String, HomeMaticConnection> connectionsMap;
	private final Object connectionLock = new Object();

	private final String port;

	public HomeMaticDriver() {
		localConnectionsMap = new HashMap<String, LocalConnection>();
		connectionsMap = new HashMap<String, HomeMaticConnection>();
		
		port = System.getProperty(CONNECTION_TYPE_KEY, CONNECTION_TYPE_SCC).toUpperCase();
		
		establishConnection();
	}

	@Override
	public DriverInfo getInfo() {
		return info;
	}

	volatile Thread scanForDevicesThread = null;

	@Override
	public void scanForDevices(final String settingsStr, final DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {
		
		listener.scanProgressUpdate(0);
		
		DeviceScanSettings settings = info.parse(settingsStr, DeviceScanSettings.class);
		boolean ignore = settings.ignoreExisting();
		
		// TODO: implement as deviceScanSettings option
		int duration = 60;
		
		LocalConnection localCon = findConnection(port);
		if (localCon != null) {
			LocalDevice localDevice = localCon.getLocalDevice();
			localDevice.setIgnoreExisting(ignore);
			localDevice.setPairing("0000000000");
			
			logger.info("Enabled Pairing for {} seconds", duration);
			for (int i = 0; i < duration; i++) {
                if (isDeviceScanInterrupted) {
                    break;
                }
				try {
					Thread.sleep(SLEEP_TIME);
					
				} catch (InterruptedException e) {
					throw new ScanInterruptedException("Unexpected interruption during device scan");
				}
				
				for (org.ogema.driver.homematic.manager.RemoteDevice rm: localDevice.getDevices().values()) {
					if (!((RemoteDevice)rm).isIgnore() && ((RemoteDevice)rm).getInitState() == InitStates.PAIRED) {
						String type = rm.getDeviceType();
						String description = localDevice.getDeviceDescriptor().getName(type) + ":" + 
								localDevice.getDeviceDescriptor().getSubType(type);
						DeviceScanInfo info = new DeviceScanInfo(rm.getAddress(), "type:" + type, description);
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
	public Connection connect(String addressStr, String settingsStr) throws ArgumentSyntaxException, ConnectionException {
		
		logger.debug("Connect HomeMatic device: ", addressStr);
		if (addressStr.length() != 6) {
			throw new ArgumentSyntaxException("Device Address has an incorrect length. The length has to be 6 Characters.");
		}
		
		HomeMaticConnection connection = connectionsMap.get(addressStr);
		if (connection == null) {
			LocalConnection localCon = findConnection(port);
			LocalDevice localDevice = localCon.getLocalDevice();		
			
			connection = new HomeMaticConnection(addressStr, this);
			connection.setLocalDevice(localDevice);
			connectionsMap.put(addressStr, connection);
			
			if (localDevice.getDevices().get(addressStr) == null) {
				DeviceSettings settings = info.parse(settingsStr, DeviceSettings.class);
				
				new RemoteDevice(localDevice, addressStr, settings.getType(), null); 
				RemoteDevice remoteDevice = (RemoteDevice) localDevice.getDevices().get(addressStr);
				SubDevice subDevice = remoteDevice.getSubDevice();
				
				if (subDevice instanceof PowerMeter || subDevice instanceof SwitchPlug) {
					BooleanValue defaultState = new BooleanValue(settings.getDefaultState());
					if (! defaultState.equals(subDevice.deviceAttributes.get((short) 0x0001).getValue())) {
						subDevice.channelChanged((byte) 0x01, defaultState);
						if (! isRemoteDeviceConnected(remoteDevice, defaultState, addressStr)) {
							connection.close();
							throw new ConnectionException("Device is not paired");
						}
					}
					else if (! isRemoteDeviceConnected(remoteDevice, null, addressStr)) {
						connection.close();
						throw new ConnectionException("Device is not paired, as no value has been sent");
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
				if (connectionTime >= CONNECT_TIMEOUT) {
					logger.debug("Device " + deviceAddressStr + " is not connected (timeout)");					
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
		LocalConnection localCon = localConnectionsMap.get(port);
		if (localCon == null) {
			try {
				if (port.equals(CONNECTION_TYPE_SCC)) {
					localCon = new LocalSccConnection(connectionLock, port, "HMSCC");
				}
				else if (port.equals(CONNECTION_TYPE_CUL)) {
					localCon = new LocalCulConnection(connectionLock, port, "HMCUL");
				}
				else {
					localCon = new LocalUsbConnection(connectionLock, port, "HMUSB");
				}
				addConnection(localCon);
			}
			catch (Exception e) {
				logger.error("Severe Error: " + e.getMessage());
				e.printStackTrace();
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
