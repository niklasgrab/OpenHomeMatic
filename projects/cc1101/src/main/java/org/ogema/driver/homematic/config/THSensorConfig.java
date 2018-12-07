package org.ogema.driver.homematic.config;

import java.util.HashMap;

public class THSensorConfig extends DeviceConfigs {

	// static final HashMap<String, HashMap<String, ListEntryValue>>
	// configsByDevice;
	static HashMap<String, ConfigListEntryValue> supportedConfigs;

	// public boolean[] pendingRegisters;

	@SuppressWarnings("unchecked")
	public THSensorConfig() {
		// pendingRegisters = new boolean[256];
		deviceConfigs = (HashMap<String, ConfigListEntryValue>) supportedConfigs.clone();		
	}
	
	static {
		// configsByDevice = new HashMap<String, HashMap<String,
		// ListEntryValue>>();

		supportedConfigs = new HashMap<String, ConfigListEntryValue>(5);
		supportedConfigs.put("burstRx", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("intKeyVisib", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("pairCentral", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("btnLock", DeviceConfigs.unInitedEntry);
		supportedConfigs.put("localResDis", DeviceConfigs.unInitedEntry);
	}

	@Override
	public HashMap<String, ConfigListEntryValue> getDeviceConfigs() {
		return deviceConfigs;
	}

}
