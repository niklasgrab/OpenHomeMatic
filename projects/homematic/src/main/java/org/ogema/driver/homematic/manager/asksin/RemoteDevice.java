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
package org.ogema.driver.homematic.manager.asksin;

import org.ogema.driver.homematic.manager.StatusMessage;
import org.ogema.driver.homematic.manager.SubDevice;
import org.ogema.driver.homematic.manager.asksin.devices.PowerMeter;
import org.ogema.driver.homematic.manager.devices.CO2Detector;
import org.ogema.driver.homematic.manager.devices.MotionDetector;
import org.ogema.driver.homematic.manager.devices.Remote;
import org.ogema.driver.homematic.manager.devices.SmokeSensor;
import org.ogema.driver.homematic.manager.asksin.devices.SwitchPlug;
import org.ogema.driver.homematic.manager.devices.THSensor;
import org.ogema.driver.homematic.manager.devices.Thermostat;
import org.ogema.driver.homematic.manager.devices.ThreeStateSensor;

/**
 * 
 * @author Godwin Burkhardt
 * 
 */
public class RemoteDevice extends org.ogema.driver.homematic.manager.RemoteDevice {

    private  int pairing = 0;
    
    private boolean ignore = false;
	
	// Used from inputhandler for new devices
	public RemoteDevice(LocalDevice localdevice, StatusMessage msg) {
		super(localdevice, msg);
	}

	// Used if device file is loading device
	public RemoteDevice(LocalDevice localdevice, String address, String type, String serial) {
		super(localdevice, address, type, serial);
	}

	@Override
	public void init() {
		setPairing(1);
		super.init();
	}

	public void initWithoutAddMandatoryChannels() {
		setPairing(1);
		String configs = "0201";
		String owner = localdevice.getOwnerid();
		configs += "0A" + owner.charAt(0) + owner.charAt(1) + "0B" + owner.charAt(2) + owner.charAt(3) + "0C"
				+ owner.charAt(4) + owner.charAt(5);
		setInitState(InitStates.PAIRING);
		pushConfig("00", "00", configs);
		// AES aktivieren
		// pushConfig("01", "01", "0801");
		pushCommand((byte) 0xA0, (byte) 0x01, "010E");
	}

	@Override
	protected SubDevice createSubDevice() {
		String s = localdevice.getDeviceDescriptor().getSubType(type);
		switch (s) {
		case "THSensor":
			return new THSensor(this);
		case "threeStateSensor":
			boolean isDoorWindowSensor = type.equals("00B1");
			return new ThreeStateSensor(this, isDoorWindowSensor);
		case "thermostat":
			return new Thermostat(this);
		case "powerMeter":
			return new PowerMeter(this);
		case "smokeDetector":
			return new SmokeSensor(this);
		case "CO2Detector":
			return new CO2Detector(this);
		case "motionDetector":
			return new MotionDetector(this);
		case "switch":
			return new SwitchPlug(this);
		case "remote":
		case "pushbutton":
		case "swi":
			return new Remote(this);
		default:
			throw new RuntimeException("Type not supported: " + s);
		}
	}


	public int getPairing() {
        return pairing;
    }
    
    public final void setPairing(int pairing) {
        this.pairing = pairing;
    }
    	
    public final void augmentPairing() {
        this.pairing++;
    }
    	
	public boolean isIgnore() {
		return ignore;
	}

	public void setIgnore(boolean ignore) {
		this.ignore = ignore;
	}
}
