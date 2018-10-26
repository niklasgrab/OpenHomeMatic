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

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ogema.driver.homematic.connection.Connection;
import org.ogema.driver.homematic.connection.ConnectionType;
import org.ogema.driver.homematic.connection.CulConnection;
import org.ogema.driver.homematic.connection.SccConnection;
import org.ogema.driver.homematic.manager.Device.InitState;
import org.ogema.driver.homematic.manager.messages.CommandMessage;
import org.ogema.driver.homematic.manager.messages.Message;
import org.ogema.driver.homematic.manager.messages.StatusMessage;
import org.ogema.driver.homematic.tools.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class handles outgoing messages that need a response.
 * 
 */
public class MessageHandler {
	private final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

	private final String CONNECTION_INTERFACE = "org.openmuc.framework.driver.homematic.interface";
	private final String CONNECTION_DEFAULT = "SCC";

	private HomeMaticManager manager;
	private InputThread inputThread;
	private Connection connection;
	private String id = null;

	private volatile Map<String, Message> sent = new LinkedHashMap<String, Message>(); // <Token>
	private volatile Map<String, OutputThread> outputThreads = new ConcurrentHashMap<String, OutputThread>();
	private volatile boolean running;

	public MessageHandler(HomeMaticManager manager) {
		this.manager = manager;
		running = true;
		inputThread = new InputThread();
		inputThread.setName("OGEMA-HomeMatic-CC1101-input-handler");
		inputThread.start();
		try {
			initialize();

		} catch (Exception e) {
			logger.error("Error while initializing manager: " + e.getMessage());
		}
	}

	protected void initialize() throws IllegalArgumentException, IOException {
		ConnectionType type = ConnectionType
				.valueOf(System.getProperty(CONNECTION_INTERFACE, CONNECTION_DEFAULT).toUpperCase());
		switch (type) {
		case CUL:
			connection = new CulConnection();
			break;
		case SCC:
			connection = new SccConnection();
			break;
		}
		connection.open();
	}

	protected boolean isReady() {
		return this.id != null;
	}

	protected void onReceivedMessage(StatusMessage msg) {
		String token = msg.source + msg.number;
		if (manager.hasDevice(msg.source)) {
			Device device = manager.getDevice(msg.source);

			// Acknowledgement received
			if (msg.type == 0x02) {
				synchronized (sent) {
					if (sent.containsKey(token) && device.getInitState() == InitState.PAIRING) {
						sent.remove(token);

						if (outputThreads.containsKey(msg.source)) {
							OutputThread thread = (OutputThread) outputThreads.get(msg.source);
							thread.interrupt();
						}
					}
				}
			} else if (!msg.destination.equals("000000")) {
				// Acknowledge message
				sendAck(msg, device);
			}

			CommandMessage cmd;
			synchronized (sent) {
				if (sent.containsKey(token)) {
					cmd = (CommandMessage) sent.get(token);
				} else {
					cmd = (CommandMessage) device.getLastMessage();
				}
				sent.remove(token);
			}
			device.parseMessage(msg, cmd, device);
		}
	}

	protected void sendAck(StatusMessage msg, Device device) {
		logger.debug("Sending acknoledgement {} to device {}", msg.number, msg.source);
		connection.sendFrame(new CommandMessage(device.getAddress(), id, (byte) 0x80, (byte) 0x02, "00")
				.getFrame(device, msg.number));
	}

	public void pushConfig(String address, String channel, String list) {
		String owner = id;
		String configs = "0201" + "0A" + owner.charAt(0) + owner.charAt(1) + "0B" + owner.charAt(2) + owner.charAt(3)
				+ "0C" + owner.charAt(4) + owner.charAt(5);

		sendMessage(address, (byte) 0xA0, (byte) 0x01, channel + "0500000000" + list);
		sendMessage(address, (byte) 0xA0, (byte) 0x01, channel + "08" + configs);
		sendMessage(address, (byte) 0xA0, (byte) 0x01, channel + "06");
	}

	public void sendMessage(String destination, byte flag, byte type, String data) {
		CommandMessage cmdMessage = new CommandMessage(destination, id, flag, type, data);
		sendMessage(cmdMessage);
	}

	public void sendMessage(String destination, byte flag, byte type, byte[] data) {
		CommandMessage cmdMessage = new CommandMessage(destination, id, flag, type, data);
		sendMessage(cmdMessage);
	}

	private void sendMessage(Message message) {
		String destination = message.getDestination();
		OutputThread thread = outputThreads.get(destination);
		if (thread == null) {
			thread = new OutputThread(destination);
			thread.setName("OGEMA-HomeMatic-CC1101-message");
			thread.start();

			outputThreads.put(destination, thread);
		}
		thread.addMessage(message);
	}

	/**
	 *   Stops the loop in run().
	 */
	public void stop() {
		connection.close();
		running = false;
		inputThread.interrupt();
		Iterator<OutputThread> it = outputThreads.values().iterator();
		while (it.hasNext()) {
			it.next().interrupt();
		}
	}

	private class OutputThread extends Thread {

		private static final int SEND_SLEEP = 2500;
		private static final int SEND_RETRIES = 4;

		private String destination;
		private volatile int tries = 0;
		private volatile int errors = 0;

		private volatile InputOutputFifo<Message> unsent; // Messages waiting to be sent

		public OutputThread(String destination) {
			this.destination = destination;
			this.unsent = new InputOutputFifo<>(8);
		}

		@Override
		public void run() {
			while (errors < 25 && running) {
				try {
					Message entry = null;
					synchronized (unsent) {
						// entry = this.unsentMessageQueue.remove(getSmallestKey());
						entry = this.unsent.get();
						if (entry == null) {
							try {
								unsent.wait();
							} catch (InterruptedException e) {
								logger.debug("Waiting message thread interrupted");
							}
							// entry = this.unsentMessageQueue.get(getSmallestKey());
							entry = this.unsent.get();
							if (entry == null)
								continue;
						}
					}
					if (!(entry instanceof CommandMessage)) {
						// should not happen
						logger.warn("Unable to handle unknown message type: {}", entry.getClass());
						continue;
					}

					Device device = manager.getDevices().get(destination);
					if (device != null) {
						CommandMessage cmd = (CommandMessage) entry;
						String token = destination + device.getMessageNumber();

						while (tries < SEND_RETRIES && running) {
							synchronized (sent) {
								if (sent.containsKey(token)) {
									sent.remove(token);
									token = destination + device.getMessageNumber();
								}
								sent.put(token, cmd);
								if (logger.isTraceEnabled()) {
									logger.trace("Add message {} to await responses: {}", token,
											sent.keySet().toString());
								}
							}
							logger.debug("Sending message {} to device {}: {}", device.getMessageNumber(), destination,
									Converter.toHexString(cmd.data));

							connection.sendFrame(cmd.getFrame(device));

							device.incMessageNumber();
							try {
								Thread.sleep(SEND_SLEEP);

							} catch (InterruptedException e) {
								// This will be interrupted when an acknowledgement is registered
							}

							if (!sent.containsKey(token)) {
								if (tries <= SEND_RETRIES) {
									logger.debug("Message sent to device {}", destination);

									if (device.getInitState() == InitState.PAIRING) {
										device.setInitState(InitState.PAIRED);
										logger.info("Successfully paired device {}", destination);
									}
								} else if (device.getInitState() == InitState.PAIRING) {
									// here we aren't sure that the device is no longer present. In case of configuration request,
									// the device wouldn't react, if the activation button is not pressed. Removing of devices
									// should be done actively by the user/administrator
									device.setInitState(InitState.UNKNOWN);
									manager.getDevices().remove(destination);
									synchronized (sent) {
										Iterator<String> it = sent.keySet().iterator();
										while (it.hasNext()) {
											if (it.next().startsWith(destination)) {
												it.remove();
											}
										}
									}
									logger.warn("Removed device {}", destination);
								}
								break;
							}
							logger.debug("Timed out while awaiting response of {}", destination);
							tries++;
						}
					}
					tries = 0;
					errors = 0;

				} catch (Exception e) {
					logger.error("Error while handling message: {}", e);
					errors++;
				}
			}
		}

		public void addMessage(Message message) {
			synchronized (unsent) {
				unsent.put(message);
				unsent.notify();
			}
		}
	}

	class InputThread extends Thread {
		private final Logger logger = LoggerFactory.getLogger(InputThread.class);

		private static final String ID_KEY = "org.openmuc.framework.driver.homematic.id";
		private static final String ID_DEFAULT = "F11034";

		private volatile Object lock;

		public InputThread() {
			lock = connection.getReceivedLock();
		}

		@Override
		public void run() {
			while (running) {
				synchronized (lock) {
					while (!connection.hasFrames()) {
						try {
							lock.wait();
						} catch (InterruptedException e1) {
						}
					}
					try {
						byte[] data = connection.getReceivedFrame();
						handleMessage(data);
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
		}

		public void handleMessage(byte[] data) {
			switch (data[0]) {
			case 'V':
				if (!isReady()) {
					parseVersion(data);
				}
				break;
			case 'a':
			case 'A':
				StatusMessage message = new StatusMessage(data);

				logger.debug("Received {} {} of type {} from device {}: {}",
						(message.destination.equals("000000") ? "broadcast" : "message"), message.number & 0x000000FF,
						message.type, message.source, Converter.toHexString(message.data));

				if (!isReady()) {
					break;
				}
				if (message.type == 0x00 & manager.getPairing() != null) { // if pairing
					Device device = Device.createDevice(manager.getDeviceDescriptor(), MessageHandler.this,
							message.source, message.parseKey(), message.parseSerial(), false);

					if (manager.getPairing().equals("0000000000") | manager.getPairing().equals(device.getSerial())) {
						if (!manager.hasDevice(device.getAddress())) {
							manager.addDevice(device);
							logger.info("Received pairing request from device: {}", device.getAddress());

							device.init();
						} else {
							device = manager.getDevice(device.getAddress());
							if (device.getInitState().equals(InitState.UNKNOWN)) {
								device.init();
							} else if (device.getInitState().equals(InitState.PAIRED)) {
								device.init(false);
							}
						}
					}
				} else {
					if (id.equals(message.destination) || message.destination.equals("000000") || message.partyMode) {
						// Destination "000000" is a broadcast
						if (manager.hasDevice(message.source)) {
							onReceivedMessage(message);
						} else {
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
			id = System.getProperty(ID_KEY, ID_DEFAULT);
			manager.setFirmware(new String(data));
			manager.setSerial("");
		}

	}
}
