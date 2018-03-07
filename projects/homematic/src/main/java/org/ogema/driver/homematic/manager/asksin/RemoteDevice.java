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
