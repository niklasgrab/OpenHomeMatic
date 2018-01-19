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

public class HomeMaticChannelPreferences {

	public static final String BAR_KEY = "bar";
	public static final int BAR_DEFAULT = 1;

	private final String addressStr;
	private final String settingsStr;
	private final Preferences settings;

	public HomeMaticChannelPreferences(String addressStr, String settingsStr, Preferences settings) {
		this.addressStr = addressStr;
		this.settingsStr = settingsStr;
		this.settings = settings;
	}

	public boolean equals(String addressStr, String settingsStr) {
		return this.addressStr.equals(addressStr) && this.settingsStr.equals(settingsStr);
	}

	public String getFoo() {
		if (addressStr != null && !addressStr.isEmpty()) {
			return addressStr.toUpperCase();
		}
		return null;
	}

	public int getBar() {
		if (settings.contains(BAR_KEY)) {
			return settings.getInteger(BAR_KEY);
		}
		return BAR_DEFAULT;
	}

}
