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
package org.openmuc.framework.driver.homematic.options;

import org.openmuc.framework.config.options.Preferences;

public class HomeMaticDevicePreferences {

	private static final String ADDRESS_KEY = "address";

	protected final Preferences address;

	public HomeMaticDevicePreferences(Preferences address) {
		this.address = address;
	}

	public String getAddress() {
		if(address.contains(ADDRESS_KEY)) {
			return address.getString(ADDRESS_KEY);
		}
		return null;
	}

}
