/*
 * Copyright 2017-18 ISC Konstanz
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

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.homematic.options.HomeMaticDevicePreferences;
import org.openmuc.framework.driver.homematic.options.HomeMaticDriverInfo;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class HomeMaticDriver implements DriverService {
	private final static Logger logger = LoggerFactory.getLogger(HomeMaticDriver.class);
    private final HomeMaticDriverInfo info = HomeMaticDriverInfo.getInfo();

	@Override
	public DriverInfo getInfo() {
		return info;
	}

	@Override
	public void scanForDevices(String settings, DriverDeviceScanListener listener)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ScanInterruptedException {
		
		// TODO Implement your scan devices method
	}

	@Override
	public void interruptDeviceScan() throws UnsupportedOperationException {
		
		// TODO Auto-generated method stub
		
		// If the driver cannot scan for devices, indicate it with an Exception
		throw new UnsupportedOperationException();
	}

	@Override
	public Connection connect(String addressStr, String settingsStr) throws ArgumentSyntaxException, ConnectionException {
		
		logger.info("Connect HomeMatic device address \"{}\": {}", addressStr, settingsStr);
        HomeMaticDevicePreferences prefs = info.getDevicePreferences(addressStr, settingsStr);
		
		try {
			return new HomeMaticDevice(prefs.getAddress());

		} catch (IllegalArgumentException e) {
			throw new ArgumentSyntaxException("Unable to configure device: " + e.getMessage());
			
		}
	}
}
