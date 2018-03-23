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

public class HomeMaticDeviceScanPreferences {

	private static final String IGNORE_EXISTING_KEY = "ignoreExisting";

	protected final Preferences settings;

	public HomeMaticDeviceScanPreferences(Preferences settings) {
		this.settings = settings;
	}

	public Boolean getIgnoreExiting() {
		if(settings.contains(IGNORE_EXISTING_KEY)) {
			return settings.getBoolean(IGNORE_EXISTING_KEY);
		}
		return null;
	}

}
