package org.ogema.driver.homematic.connection;

import java.io.IOException;
import java.util.TooManyListenersException;

import org.ogema.driver.homematic.Constants;
import org.ogema.driver.homematic.connection.serial.SccConnection;
import org.ogema.driver.homematic.manager.asksin.LocalDevice;

public class LocalSccConnection extends LocalConnection {

	private final SccConnection connection;

	public LocalSccConnection(Object lock, String iface, String parameter) {
		super(lock, iface, parameter);
		
		connection = new SccConnection(ProtocolType.ASKSIN);
		startConnectThread();
	}
	
	private void startConnectThread() {
		Thread connectScc = new Thread() {
			@Override
			public void run() {
				while (!hasConnection) {
					try {
						connection.open();
//						serialConnection.setAskSinMode(true);
						synchronized (connectionLock) {
							hasConnection = true;
							localDevice = new LocalDevice(parameterString, connection, ProtocolType.ASKSIN);
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
		connectScc.setName("OGEMA-HomeMatic-CC1101-SCC-connect");
		connectScc.start();
	}
}
