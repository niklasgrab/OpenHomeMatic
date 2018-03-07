package org.ogema.driver.homematic.connection;

import org.ogema.driver.homematic.manager.asksin.LocalDevice;

public abstract class LocalConnection {

	final String interfaceId;
	final String parameterString;
	protected LocalDevice localDevice;
//	private final Logger logger = org.slf4j.LoggerFactory.getLogger("homematic-driver");
	boolean hasConnection = false;
	final Object connectionLock;

	public LocalConnection(Object lock, String iface, String parameter) {
		this.connectionLock = lock;
		interfaceId = iface;
		parameterString = parameter;
	}

	public String getInterfaceId() {
		return interfaceId;
	}

	public LocalDevice getLocalDevice() {
		return localDevice;
	}

	public void close() {
		localDevice.close();
	}

	public boolean hasConnection() {
		return hasConnection;
	}
}
