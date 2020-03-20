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
package org.openmuc.framework.driver.homematic;

import java.util.ArrayList;
import java.util.List;

import org.ogema.driver.homematic.HomeMaticException;
import org.ogema.driver.homematic.HomeMaticManager;
import org.ogema.driver.homematic.data.BooleanValue;
import org.ogema.driver.homematic.data.Value;
import org.ogema.driver.homematic.device.Device;
import org.ogema.driver.homematic.device.SwitchMeterPlug;
import org.ogema.driver.homematic.device.SwitchPlug;
import org.openmuc.framework.driver.Driver;
import org.openmuc.framework.driver.DriverContext;
import org.openmuc.framework.driver.homematic.device.DeviceChannels;
import org.openmuc.framework.driver.homematic.device.DevicePairing;
import org.openmuc.framework.driver.homematic.device.DeviceSettings;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.DriverService;
import org.osgi.service.component.annotations.Component;

@Component(service = DriverService.class)
public class HomeMaticDriver extends Driver<DeviceSettings> {

    private static final String ID = "homematic-cc1101";
    private static final String NAME = "HomeMatic CC1101";
    private static final String DESCRIPTION = "The HomeMatic CC1101 Driver implements the communication with " + 
    		"<a href='https://www.eq-3.de/produkte/homematic.html'>Smart Home devices by eQ-3</a> " + 
    		"over CC1101 transceivers.";

	private List<String> connected = new ArrayList<String>();

	private HomeMaticManager manager;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    protected void onCreate(DriverContext context) {
        context.setName(NAME)
               .setDescription(DESCRIPTION)
               .setDeviceScanner(DevicePairing.class)
        	   .setChannelScanner(DeviceChannels.class);
    }

    @Override
    protected void onActivate() {
		manager = new HomeMaticManager();
    }

    @Override
    protected void onDeactivate() {
    	manager.close();
    }

    @Override
    protected DevicePairing onCreateScanner(String settings) {
    	return new DevicePairing(manager, connected);
    }

    @Override
    protected HomeMaticConnection onCreateConnection(DeviceSettings settings) throws ConnectionException {
    	String id = settings.getId();
    	Device device = manager.getDevice(id);
		try {
			if (device == null) {
				device = manager.addDevice(id, settings.getType());
			}
			if (device instanceof SwitchMeterPlug || device instanceof SwitchPlug) {
				if (settings.hasDefaultState()) {
					Value value = device.getAttributeValue((short) 0x0001);
					if (value == null || value.asBoolean() != settings.getDefaultState()) {
						device.sendCommand((byte) 0x01, new BooleanValue(settings.getDefaultState()));
					}
				}
			}
		} catch (HomeMaticException e) {
			throw new ConnectionException(e.getMessage());
		}
    	return new HomeMaticConnection(device);
    }

    @Override
    protected void onConnect(Connection connection) {
		connected.add(((HomeMaticConnection) connection).device.getId());
    }

    @Override
    protected void onDisconnect(Connection connection) {
		connected.remove(((HomeMaticConnection) connection).device.getId());
    }

}
