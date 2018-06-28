package org.ogema.driver.homematic.manager.asksin.devices;

import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.driver.homematic.manager.RemoteDevice;

public class PowerMeter extends org.ogema.driver.homematic.manager.devices.PowerMeter {

	public PowerMeter(RemoteDevice rd) {
		super(rd);
	}

	@Override
	public void channelChanged(byte identifier, Value value) {
		if (identifier == 0x01) {
			BooleanValue v = (BooleanValue)value;
			this.remoteDevice.pushCommand((byte) 0xA0, (byte) 0x11, 
					"0201" + ((v.getBooleanValue()) ? "C8" : "00") + "0000");
			// Get state
			this.remoteDevice.pushCommand((byte) 0xA0, (byte) 0x01, "010E");
		}
	}
}
