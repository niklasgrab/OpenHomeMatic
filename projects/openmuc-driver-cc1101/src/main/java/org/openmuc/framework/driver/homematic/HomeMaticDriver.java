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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.DriverInfoFactory;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.homematic.HomeMaticConnection.HomeMaticConnectionCallbacks;
import org.ogema.driver.homematic.HomeMaticConnectionException;
import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.Device.InitState;
import org.ogema.driver.homematic.manager.HomeMaticManager;
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
	private static final Logger logger = LoggerFactory.getLogger(HomeMaticDriver.class);

	private final DriverInfo info = DriverInfoFactory.getPreferences(HomeMaticDriver.class);

	private int SLEEP_TIME = 1000;

	private HomeMaticManager manager;

	private final Map<String, HomeMaticConnection> connections = new HashMap<String, HomeMaticConnection>();

	private volatile boolean isDeviceScanInterrupted = false;

	public HomeMaticDriver() {
		manager = new HomeMaticManager();
	}

	@Override
	public DriverInfo getInfo() {
		return info;
	}

	@Override
	public void scanForDevices(String settingsStr, DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {

		DeviceScanSettings settings = info.parse(settingsStr, DeviceScanSettings.class);
		
		// TODO: implement as deviceScanSettings option
		int duration = 60;
		
		List<String> ignore = new ArrayList<String>();
		if (settings.ignoreExisting()) {
			ignore.addAll(connections.keySet());
		}
		logger.info("Enabled Pairing for {} seconds", duration);
		
		manager.setPairing("0000000000");
		
		for (int i = 0; i <= duration; i++) {
			if (isDeviceScanInterrupted) {
				break;
			}
			for (Device device: manager.getDevices().values()) {
				if (device.getInitState() == InitState.PAIRED && !ignore.contains(device.getAddress())) {
					String key = device.getKey();
					String address = device.getAddress();
					String description = manager.getDeviceDescriptor().getName(key);
					
					DeviceScanInfo info = new DeviceScanInfo(address, "type:" + key, description);
					listener.deviceFound(info);
					
					ignore.add(address);
				}
			}
			listener.scanProgressUpdate((int) Math.round(i/(double) duration*100));
			
			try {
				Thread.sleep(SLEEP_TIME);
				
			} catch (InterruptedException e) {
				throw new ScanInterruptedException("Unexpected interruption during device scan");
			}
		}
		manager.setPairing(null);
		logger.info("Pairing disabled.");
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
		
		HomeMaticConnection connection = connections.get(addressStr);
		if (connection == null) {
			DeviceSettings settings = info.parse(settingsStr, DeviceSettings.class);
			Device device;
			if (manager.hasDevice(addressStr)) {
				device = manager.getDevice(addressStr);
			}
			else {
				try {
					device = manager.addDevice(addressStr, settings.getType());
				} catch (HomeMaticConnectionException e) {
					throw new ConnectionException(e.getMessage());
				}
			}
			connection = new HomeMaticConnection(this, device, settings);
			connections.put(addressStr, connection);
		}
		return connection;
	}

	@Override
	public void onDisconnect(String deviceAddress) {
		connections.remove(deviceAddress);
	}

}
