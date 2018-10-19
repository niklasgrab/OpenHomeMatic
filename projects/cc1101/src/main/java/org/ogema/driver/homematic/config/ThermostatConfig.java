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

/**
 * This class provides a list of configurations, that are supported by a type of
 * device. The list key is the unique name of the configuration. For each
 * physical instance of the device the list is cloned and fulfilled with the
 * data provided by the physical device.
 *
 */
public class ThermostatConfig extends DeviceConfigs {
	// static final HashMap<String, HashMap<String, ListEntryValue>>
	// configsByDevice;
	static HashMap<String, ConfigListEntryValue> supportedConfigs;

	// public boolean[] pendingRegisters;

	@SuppressWarnings("unchecked")
	public ThermostatConfig() {
		// pendingRegisters = new boolean[256];
		deviceConfigs = (HashMap<String, ConfigListEntryValue>) supportedConfigs.clone();
	}

	static {
		// configsByDevice = new HashMap<String, HashMap<String,
		// ListEntryValue>>();

		supportedConfigs = new HashMap<String, ConfigListEntryValue>(43);
		supportedConfigs.put("backOnTime", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("btnLock", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("btnNoBckLight", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("burstRx", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("cyclicInfoMsg", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("cyclicInfoMsgDis", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("globalBtnLock", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("localResDis", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("lowBatLimitRT", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("modusBtnLock", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("sign", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("dayTemp", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("nightTemp", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("tempMin", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("tempMax", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("tempOffset", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("decalcWeekday", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("decalcTime", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("boostPos", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("boostPeriod", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("daylightSaveTime", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("regAdaptive", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("showInfo", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("noMinMax4Manu", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("showWeekday", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("valveOffsetRt", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("valveMaxPos", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("valveErrPos", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("modePrioManu", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("modePrioParty", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("reguIntI", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("reguIntP", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("reguIntPstart", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("reguExtI", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("reguExtP", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("reguExtPstart", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("winOpnTemp", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("winOpnPeriod", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("winOpnBoost", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("winOpnMode", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("winOpnDetFall", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("CtrlRc", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("TempRC", DeviceConfigs.unInitedEntry);
	}

	@Override
	public HashMap<String, ConfigListEntryValue> getDeviceConfigs() {
		return deviceConfigs;
	}
}
