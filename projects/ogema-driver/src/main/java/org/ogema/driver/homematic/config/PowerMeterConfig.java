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
 * This class provides a list of configurations, that are supported by a type of device. The list key is the unique name
 * of the configuration. For each physical instance of the device the list is cloned and fulfilled with the data
 * provided by the physical device.
 *
 */
public class PowerMeterConfig extends DeviceConfigs {
	// static final HashMap<String, HashMap<String, ListEntryValue>>
	// configsByDevice;
	static HashMap<String, ConfigListEntryValue> supportedConfigs;

	// public boolean[] pendingRegisters;

	@SuppressWarnings("unchecked")
	public PowerMeterConfig() {
		// pendingRegisters = new boolean[256];
		deviceConfigs = (HashMap<String, ConfigListEntryValue>) supportedConfigs.clone();
	}

	static {
		// configsByDevice = new HashMap<String, HashMap<String,
		// ListEntryValue>>();

		supportedConfigs = new HashMap<String, ConfigListEntryValue>(43);
		supportedConfigs.put("ActionType", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("averaging", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("cndTxCycAbove", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("cndTxCycBelow", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("cndTxDecAbove", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("cndTxDecBelow", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("cndTxFalling", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("cndTxRising", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("CtDlyOff", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("CtDlyOn", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("CtOff", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("CtOn", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("CtValHi", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("CtValLo", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("expectAES", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("ledOnTime", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("lgMultiExec", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("OffDly", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("OffTime", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("OffTimeMode", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("OnDly", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("OnTime", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("OnTimeMode", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("peerNeedsBurst", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("powerUpAction", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("sign", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("statusInfoMinDly", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("statusInfoRandom", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("SwJtDlyOff", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("SwJtDlyOn", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("SwJtOff", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("SwJtOn", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("transmitTryMax", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txMinDly", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrCur", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrFrq", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrHiCur", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrHiFrq", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrHiPwr", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrHiVlt", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrLoCur", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrLoFrq", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrLoPwr", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrLoVlt", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrPwr", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("txThrVlt", DeviceConfigs.unInitedEntry);
	}

	@Override
	public HashMap<String, ConfigListEntryValue> getDeviceConfigs() {
		return deviceConfigs;
	}
}
