package org.ogema.driver.homematic.connection;

import java.io.IOException;
import java.util.TooManyListenersException;

import org.ogema.driver.homematic.Constants;
import org.ogema.driver.homematic.connection.serial.SerialConnection;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;

public class LocalSerialConnection  extends LocalConnection {
	
	private final SerialConnection serialConnection;

	public LocalSerialConnection(Object lock, String iface, String parameter) {
		super(lock, iface, parameter);
		
		serialConnection = new SerialConnection(ProtocolType.asksin);
		startConnectThread();
	}
	
	private void startConnectThread() {
		Thread connectSerial = new Thread() {
			@Override
			public void run() {
				while (!hasConnection) {
					try {
						serialConnection.open();
//						serialConnection.setAskSinMode(true);
						synchronized (connectionLock) {
							hasConnection = true;
							localDevice = new LocalDevice(parameterString, serialConnection,ProtocolType.asksin);
							while (!localDevice.getIsReady()) {
								try {
									Thread.sleep(Constants.CONNECT_WAIT_TIME/10);
								} catch (InterruptedException ie) {
									ie.printStackTrace();
								}
							}
							connectionLock.notify();
						}
					} catch (IOException | TooManyListenersException e) {
						try {
							Thread.sleep(Constants.CONNECT_WAIT_TIME);
						} catch (InterruptedException ie) {
							ie.printStackTrace();
						}
					}
				}
			}
		};
		connectSerial.setName("homematic-ll-connectSerial");
		connectSerial.start();
	}
}
