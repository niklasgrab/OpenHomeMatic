/**
 * This file is part of OGEMA.
 *
 * OGEMA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3
 * as published by the Free Software Foundation.
 *
 * OGEMA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OGEMA. If not, see <http://www.gnu.org/licenses/>.
 */
package org.ogema.driver.homematic.config;

import java.util.HashMap;

public class WaterDetectorConfig extends DeviceConfigs {
	static HashMap<String, ConfigListEntryValue> supportedConfigs;

	HashMap<String, ConfigListEntryValue> deviceConfigs;

	@SuppressWarnings("unchecked")
	public WaterDetectorConfig() {
		deviceConfigs = (HashMap<String, ConfigListEntryValue>) supportedConfigs.clone();
	}

	static {
		supportedConfigs = new HashMap<String, ConfigListEntryValue>(4);
		supportedConfigs.put("msgWdsPosA", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("msgWdsPosB", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("msgWdsPosC", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("eventFilterTimeB", DeviceConfigs.unInitedEntry);
	}

	public HashMap<String, ConfigListEntryValue> getDeviceConfigs() {
		return deviceConfigs;
	}
}
