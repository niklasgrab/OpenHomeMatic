package org.ogema.driver.homematic.connection;

import org.ogema.driver.homematic.Constants;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;
import org.ogema.driver.homematic.usbconnection.UsbConnection;

public class LocalUsbConnection extends LocalConnection {

	public LocalUsbConnection(Object lock, String iface, String parameter) {
		super(lock, iface, parameter);

		final UsbConnection usbConnection = new UsbConnection();
		

		Thread connectUsb = new Thread() {
			@Override
			public void run() {
				while (!hasConnection) {
					if (usbConnection.connect()) {
						synchronized (connectionLock) {
							hasConnection = true;
							localDevice = new LocalDevice(parameterString, usbConnection, ProtocolType.other);
							connectionLock.notify();
						}
					}
					try {
						Thread.sleep(Constants.CONNECT_WAIT_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		};
		connectUsb.setName("homematic-ll-connectUSB");
		connectUsb.start();
	}
}
