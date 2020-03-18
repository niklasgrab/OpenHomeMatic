/*
 * Copyright 2016-19 ISC Konstanz
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

import org.openmuc.framework.driver.DeviceConfigs;
import org.openmuc.framework.options.Address;
import org.openmuc.framework.options.Setting;

public class DeviceSettings extends DeviceConfigs<DeviceChannel> {

	@Address(id = "id",
			name = "Identifier",
			description = "The unique identifier of the HomeMatic device")
	private String id;

	@Setting(id = "type",
			name = "Type",
			description = "The hex code identifier of the HomeMatic device type")
	private String type;

	@Setting(id = "defaultState",
			name = "Default State",
			description = "The default state of the HomeMatic device, the driver will attempt to set when starting up.<br>" + 
						"This may be applicable e.g. for some Smart Plugs or Switches after a blackout.",
			mandatory = false)
	private Boolean defaultState = null;

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public boolean hasDefaultState() {
		return defaultState != null;
	}

	public boolean getDefaultState() {
		return defaultState;
	}

}
