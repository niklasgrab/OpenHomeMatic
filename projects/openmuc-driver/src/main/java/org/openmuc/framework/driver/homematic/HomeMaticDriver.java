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

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.config.options.Preferences;
import org.ogema.driver.homematic.Activator;
import org.ogema.driver.homematic.connection.LocalConnection;
import org.ogema.driver.homematic.connection.LocalSerialConnection;
import org.ogema.driver.homematic.connection.LocalUsbConnection;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;
import org.ogema.driver.homematic.manager.asksin.RemoteDevice;
import org.openmuc.framework.driver.homematic.options.HomeMaticDeviceOptions;
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
public class HomeMaticDriver implements DriverService {

	private final static Logger logger = LoggerFactory.getLogger(HomeMaticDriver.class);

	public final static DriverInfo DRIVER_INFO = new DriverInfo(HomeMaticDriver.class.getResourceAsStream("options.xml"));

	public static final String USB_CONNECTION_TYPE_KEY = "usbConnectionType";
	public static final String USB_CONNECTION_TYPE_SERIAL = "serial";

	private final Map<String, LocalConnection> localConnectionsMap; // <interfaceId, connection>
	private final Object connectionLock = new Object();
	private final Map<String, HomeMaticConnection> connectionsMap;
	private final String portname;


	public HomeMaticDriver() {
		localConnectionsMap = new HashMap<String, LocalConnection>();
		connectionsMap = new HashMap<String, HomeMaticConnection>();
		
		portname = System.getProperty(USB_CONNECTION_TYPE_KEY, USB_CONNECTION_TYPE_SERIAL);
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
		
//		scanForDevicesThread = new Thread() {
//			@Override
//			public void run() {
				Preferences settings = null;
				try {
					settings = DRIVER_INFO.parseDeviceScanSettings(settingsStr);
				} catch (ArgumentSyntaxException e) {
					logger.info("Can't parse Device Scan Settings: " + e.getMessage());
					e.printStackTrace();
				}
				
				LocalConnection localCon = findConnection(portname);			

				boolean ignoreExisting = true;
				if (settings != null) {
					try {
						ignoreExisting = settings.getBoolean(HomeMaticDeviceOptions.IGNORE_EXISTING_KEY);
					}
					catch (NullPointerException e) {
						
					}
				}
				enablePairing(portname, ignoreExisting, listener);		
				
				LocalDevice localDevice = localCon.getLocalDevice();
				for (org.ogema.driver.homematic.manager.RemoteDevice rm: localDevice.getDevices().values()) {
					if (!((RemoteDevice)rm).isIgnore()) {
						String type = rm.getDeviceType();
						String description = localDevice.getDeviceDescriptor().getName(type) + ":" + 
								localDevice.getDeviceDescriptor().getSubType(type);
						DeviceScanInfo info = new DeviceScanInfo(rm.getAddress(), "deviceType=" + type, description);
						listener.deviceFound(info);
					}
				}
				
//			}
//		};
		listener.scanProgressUpdate(0);
//		scanForDevicesThread.setName("homematic-ll-scanForDevices");
//		scanForDevicesThread.start();
	}

	@Override
	public void interruptDeviceScan() throws UnsupportedOperationException {
		
		// TODO Auto-generated method stub
		
		// If the driver cannot scan for devices, indicate it with an Exception
		throw new UnsupportedOperationException();
	}

	@Override
	public Connection connect(String deviceAddressStr, String settingsStr) throws ArgumentSyntaxException, ConnectionException {
		
		logger.info("Connect HomeMatic device: ", deviceAddressStr);
		
		HomeMaticConnection connection = connectionsMap.get(deviceAddressStr);
		
		if (connection == null) {
			LocalConnection localCon = findConnection(portname);			
			connection = new HomeMaticConnection(deviceAddressStr);
			connectionsMap.put(deviceAddressStr, connection);
			LocalDevice localDevice = localCon.getLocalDevice();
			connection.setLocalDevice(localDevice);
			if (localDevice.getDevices().get(deviceAddressStr) == null) {
				Preferences settings = DRIVER_INFO.parseDeviceSettings(settingsStr);
				RemoteDevice remoteDevice = new RemoteDevice(localDevice, deviceAddressStr, 
						settings.getString(HomeMaticDeviceOptions.DEVICE_TYPE_KEY), null); 
				localDevice.getDevices().put(remoteDevice.getAddress(), remoteDevice);
				remoteDevice.init();
			}
		}

		return connection;

	}

	public void enablePairing(final String iface, boolean ignoreExisting, DriverDeviceScanListener listener) {
		try {
			// TODO: dirty connection
			LocalConnection localCon = findConnection(iface);
			if (localCon != null) {
				localCon.getLocalDevice().setIgnoreExisting(ignoreExisting);
				localCon.getLocalDevice().setPairing("0000000000");
				logger.info("enabled Pairing for 60 seconds");
				Map<String, RemoteDevice> foundRemoteDevices = new HashMap<String, RemoteDevice>();
//				for (int i = 0; i < 10; i++) {
//					Thread.sleep(6000);
//					HMLocalDevice localDevice = localCon.getLocalDevice();
//					for (HMRemoteDevice rm: localDevice.getDevices().values()) {
//						if (!rm.isIgnore() && !foundRemoteDevices.containsKey(rm.getAddress())) {
//							foundRemoteDevices.put(rm.getAddress(), rm);
//							String type = rm.getDeviceType();
//							String description = localDevice.getDeviceDescriptor().getName(type) + ":" + 
//									localDevice.getDeviceDescriptor().getSubType(type);
//							DeviceScanInfo info = new DeviceScanInfo(rm.getAddress(), "deviceType=" + type, description);
//							listener.deviceFound(info);
//						}
//					}

					Thread.sleep(20000);
					listener.scanProgressUpdate(100);
//					listener.scanProgressUpdate((i+1)*10);
//				}
				localCon.getLocalDevice().setPairing(null);
				logger.info("Pairing disabled.");
			}
		} catch (Exception e) {
			logger.error("Severe Error: " + e.getMessage());
			 e.printStackTrace();
		}
	}

	private void establishConnection() {
		final String parameter = "HMUSB";
		LocalConnection localCon = localConnectionsMap.get(portname);
		if (localCon == null) {
			try {
				if (portname.equals(USB_CONNECTION_TYPE_SERIAL)) {
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

	protected void removeConnection(String interfaceId) {
		localConnectionsMap.remove(interfaceId);
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
					// ex.printStackTrace();
				}
			}
		}
		return localCon;
	}

}
