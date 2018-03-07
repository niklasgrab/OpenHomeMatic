package org.openmuc.framework.driver.homematic;

import org.ogema.core.channelmanager.driverspi.DeviceLocator;
import org.ogema.driver.homematic.manager.RemoteDevice;

/**
 * 
 * @author baerthbn
 * 
 */
public class Device extends org.ogema.driver.homematic.Device {
	private HomeMaticConnection con;

	public Device(DeviceLocator deviceLocator, HomeMaticConnection connection) {
		super(deviceLocator, null);
		con = connection;
	}

	public RemoteDevice getRemoteDevice() {
		return con.getLocalDevice().getDevices().get(getDeviceAddress());
	}

}
