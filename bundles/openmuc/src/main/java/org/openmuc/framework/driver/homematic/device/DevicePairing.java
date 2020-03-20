/*
 * Copyright 2016-20 ISC Konstanz
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
package org.openmuc.framework.driver.homematic.device;

import java.util.ArrayList;
import java.util.List;

import org.ogema.driver.homematic.HomeMaticManager;
import org.ogema.driver.homematic.device.Device;
import org.ogema.driver.homematic.device.Device.DeviceState;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DeviceScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.config.ScanInterruptedException;
import org.openmuc.framework.driver.DeviceScanner;
import org.openmuc.framework.driver.spi.DriverDeviceScanListener;
import org.openmuc.framework.options.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DevicePairing extends DeviceScanner {
	private static final Logger logger = LoggerFactory.getLogger(DevicePairing.class);

	private static final int SLEEP_TIME = 1000;

    @Setting(id = "duration",
            name = "Duration",
            description = "The duration pairing will be enabled.",
            valueDefault = "60",
            mandatory = false
    )
	private int duration = 60;

    @Setting(id = "ignoreExisting",
            name = "Ignore Existing",
            description = "Ignore already configured HomeMatic devices when scanning.<br>" + 
            		"If disabled, all already paired and registered devices will be listed as well.",
            valueDefault = "true",
            mandatory = false
    )
    private boolean ignoreExisting = true;

	private final List<String> ignore = new ArrayList<String>();

	private HomeMaticManager manager;

    private volatile boolean interrupt = false;

    public DevicePairing(HomeMaticManager manager, List<String> ignore) {
    	this.manager = manager;
    	this.ignore.addAll(ignore);
    }

	@Override
	public void onScan(DriverDeviceScanListener listener) 
			throws ArgumentSyntaxException, ScanException, ScanInterruptedException {
		logger.info("Enabled Pairing for {} seconds", duration);
		
        interrupt = false;
        
		manager.setPairing("0000000000");
		for (int i=0; i<=duration; i++) {
			if (interrupt) {
				break;
			}
			for (Device device: manager.getDevices().values()) {
				if (device.getState() == DeviceState.PAIRED && !ignore.contains(device.getId())) {
					DeviceScanInfo info = new DeviceScanInfo(device.getId(), "type:" + device.getType(), device.getName());
					listener.deviceFound(info);
					ignore.add(device.getId());
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
		logger.debug("Pairing disabled.");
	}

	@Override
	public void onScanInterrupt() {
    	interrupt = true;
	}

}
