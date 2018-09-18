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
package org.ogema.driver.homematic.manager;

import java.util.Arrays;

import org.ogema.driver.homematic.manager.Device.InitState;
import org.ogema.driver.homematic.manager.messages.StatusMessage;
import org.ogema.driver.homematic.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InputHandler implements Runnable {
	private final Logger logger = LoggerFactory.getLogger(InputHandler.class);

	private static final String ID_KEY = "org.openmuc.framework.driver.homematic.id";
	private static final String ID_DEFAULT = "F11034";

	private volatile boolean running;
	private volatile Object lock;
	private Thread thread;

	private HomeMaticManager manager;

	public InputHandler(HomeMaticManager manager) {
		lock = manager.getReceivedLock();
		running = true;
		
		this.manager = manager;
	}

	/**
	 *   Starts the input handler thread.
	 */
	public void start() {
		thread = new Thread(this);
		thread.setName("OGEMA-HomeMatic-CC1101-input-handler");
		thread.start();
	}

	/**
	 *   Stops the loop in run().
	 */
	public void stop() {
		running = false;
		thread.interrupt();
	}

	@Override
	public void run() {
		while (running) {
			synchronized (lock) {
				while (!manager.hasReceivedFrames()) {
					try {
						lock.wait();
					} catch (InterruptedException e1) {
					}
				}
				try {
					handleMessage(manager.getReceivedFrame());
				} catch (Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

	public void handleMessage(byte[] data) {
		switch (data[0]) {
		case 'V':
			if (!manager.isReady()) {
				parseVersion(data);
			}
			break;
		case 'a':
		case 'A':
			StatusMessage message = new StatusMessage(data);
			
			logger.debug("Received {} {} of type {} from device {}: {}",
					(message.destination.equals("000000") ? "broadcast" : "message"),
					message.number & 0x000000FF, message.type, message.source, 
					Converter.toHexString(message.data));
			
			if (!manager.isReady()) {
				break;
			}
			if (message.type == 0x00 & manager.getPairing() != null) { // if pairing
				Device device = new Device(manager, message);
				
				if (manager.getPairing().equals("0000000000") | manager.getPairing().equals(device.getSerial())) {
					if (!manager.hasDevice(device.getAddress())) {
						manager.addDevice(device);
						logger.info("Received pairing request from device: {}", device.getAddress());
						
						device.init();
					}
					else {
						device = manager.getDevice(device.getAddress());
						if (device.getInitState().equals(InitState.UNKNOWN)) {
							device.init();
						}
						else if (device.getInitState().equals(InitState.PAIRED)) {
							device.init(false);
						}
					}
				}
			}
			else {
				if (manager.getId().equals(message.destination) || message.destination.equals("000000") || message.partyMode) {
					// Destination "000000" is a broadcast
					if (manager.hasDevice(message.source)) {
						manager.onReceivedMessage(message);
					}
					else {
						logger.debug("Received message from unpaired device: {}", message.source);
					}
				}
			}
			break;
		default:
			logger.debug("Unknown message: " + Converter.dumpHexString(data));
			break;
		}
	}

	private void parseVersion(byte[] data) {
		// remove \r\n
		data = Arrays.copyOfRange(data, 0, 13);
		logger.info("Registered manager: {}", new String(data));
		
		// Used in command messages
		manager.setId(System.getProperty(ID_KEY, ID_DEFAULT));
		manager.setFirmware(new String(data));
		manager.setSerial("");
	}

}
