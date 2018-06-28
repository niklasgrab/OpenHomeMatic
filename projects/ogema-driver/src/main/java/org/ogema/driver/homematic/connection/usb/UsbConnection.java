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
package org.ogema.driver.homematic.connection.usb;

import org.ogema.driver.homematic.connection.ProtocolType;

public class UsbConnection extends org.ogema.driver.homematic.usbconnection.UsbConnection {

	private ProtocolType protocolType = ProtocolType.BYTE;

	public UsbConnection(final ProtocolType type) {
		super();
		protocolType = type;
	}

	public ProtocolType getProtocolType() {
		return protocolType;
	}

}
