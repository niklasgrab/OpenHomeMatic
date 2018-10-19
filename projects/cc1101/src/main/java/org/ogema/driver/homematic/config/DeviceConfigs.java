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
import java.util.Map;

/**
 * This is the super class for all device specific configuration classes. It holds the raw data of all supported
 * registers in all lists. They are initialized at the first full configuration reading and updated after each change
 * trough a configuration write operation.
 *
 */
public abstract class DeviceConfigs {
	static final HashMap<String, Class<?>> configsByType = new HashMap<String, Class<?>>();
	static {
		configsByType.put("HM-CC-RT-DN", ThermostatConfig.class);
		configsByType.put("HM-SEC-WDS-2", WaterDetectorConfig.class);
		configsByType.put("HM-ES-PMSw1-Pl", PowerMeterConfig.class);
	}

	public abstract HashMap<String, ConfigListEntryValue> getDeviceConfigs();

	/**
	 * Raw data provided by the device consisting of (register, value) pairs.
	 */
	public HashMap<Integer, Integer> rawRegValues0;
	public HashMap<Integer, Integer> rawRegValues1;
	public HashMap<Integer, Integer> rawRegValues3;
	public HashMap<Integer, Integer> rawRegValues4;
	public HashMap<Integer, Integer> rawRegValues5;
	public HashMap<Integer, Integer> rawRegValues6;
	public HashMap<Integer, Integer> rawRegValues7;

	HashMap<String, ConfigListEntryValue> deviceConfigs;

	static final ConfigListEntryValue unInitedEntry = new ConfigListEntryValue(null, 0);

	public Map<Integer, Integer> getRegValues(int list) {
		Map<Integer, Integer> result = null;
		switch (list) {
		case 0:
			if (rawRegValues0 == null)
				rawRegValues0 = new HashMap<Integer, Integer>();
			result = rawRegValues0;
			break;
		case 1:
			if (rawRegValues1 == null)
				rawRegValues1 = new HashMap<Integer, Integer>();
			result = rawRegValues1;
			break;
		case 3:
			if (rawRegValues3 == null)
				rawRegValues3 = new HashMap<Integer, Integer>();
			result = rawRegValues3;
			break;
		case 4:
			if (rawRegValues4 == null)
				rawRegValues4 = new HashMap<Integer, Integer>();
			result = rawRegValues4;
			break;
		case 5:
			if (rawRegValues5 == null)
				rawRegValues5 = new HashMap<Integer, Integer>();
			result = rawRegValues5;
			break;
		case 6:
			if (rawRegValues6 == null)
				rawRegValues6 = new HashMap<Integer, Integer>();
			result = rawRegValues6;
			break;
		case 7:
			if (rawRegValues7 == null)
				rawRegValues7 = new HashMap<Integer, Integer>();
			result = rawRegValues7;
			break;
		}
		return result;
	}

	public ConfigListEntryValue getEntryValue(ConfigListEntry e) {
		ConfigListEntryValue lev = deviceConfigs.get(e.name);
		if (lev == unInitedEntry) {
			lev = new ConfigListEntryValue(null, -1);
			deviceConfigs.put(e.name, lev);
			lev.entry = e;
		}
		return lev;
	}

	public static DeviceConfigs getConfigs(String name) {
		Class<?> cls = configsByType.get(name);
		if (cls == null)
			cls = AllConfigs.class;
		DeviceConfigs result = null;
		try {
			result = (DeviceConfigs) cls.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			e.printStackTrace();
		}
		return result;
	}
}
