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
package org.ogema.driver.homematic.com.message;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.ogema.driver.homematic.HomeMaticException;
import org.ogema.driver.homematic.HomeMaticManager;
import org.ogema.driver.homematic.com.Connection;
import org.ogema.driver.homematic.com.ConnectionType;
import org.ogema.driver.homematic.com.CulConnection;
import org.ogema.driver.homematic.com.SccConnection;
import org.ogema.driver.homematic.data.TypeConverter;
import org.ogema.driver.homematic.device.Device;
import org.ogema.driver.homematic.device.Device.DeviceState;
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
	private int numberOfPushConfigs = 0;

	private volatile Map<String, Message> sent = new LinkedHashMap<String, Message>(); // <Token>
	private volatile Map<String, MessageThread> outputThreads = new ConcurrentHashMap<String, MessageThread>();
	private boolean running;

	public MessageHandler(HomeMaticManager manager) {
		this.manager = manager;
		running = true;
		try {
			initialize();
			
		} catch (Exception e) {
			logger.error("Error while initializing manager: " + e.getMessage());
		}
	}

	protected void initialize() throws IllegalArgumentException, IOException {
		ConnectionType type = ConnectionType.valueOf(System.getProperty(CONNECTION_INTERFACE, CONNECTION_DEFAULT).toUpperCase());
		switch (type) {
		case CUL:
			connection = new CulConnection();
			break;
		case SCC:
			connection = new SccConnection();
			break;
		}
		inputThread = new InputThread();
		inputThread.setName("OGEMA-HomeMatic-CC1101-input-handler");
		inputThread.start();
		connection.open();
		Thread sendVersionRequestThread = new SendVersionRequestThread();
		sendVersionRequestThread.setName("OGEMA-HomeMatic-CC1101-send-version-request");
		sendVersionRequestThread.start();
	}

	private class SendVersionRequestThread extends Thread {
		
		private static final int OPEN_SLEEP = 1000;
		private static final int OPEN_RETRIES = 10;

		@Override
		public void run() {
			for (int i = 0; i < OPEN_RETRIES; i++) {
				if (!isReady()) {
//						Send "Ar" to enable Asksin mode and request version info "V" to verify connection
//						connection.sendFrame("Ar".getBytes());
						connection.sendFrame(new byte[] {(byte) 0x41, (byte) 0x72});
//						connection.sendFrame("V".getBytes());
						connection.sendFrame(new byte[] {(byte) 0x56});
				}
				else {
					break;
				}
				try {
					Thread.sleep(OPEN_SLEEP);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String getId() {
		return id;
	}

	public boolean isReady() {
		return this.id != null;
	}

	protected void onReceivedMessage(StatusMessage msg) {
		String token = msg.source + msg.number;
		if (manager.hasDevice(msg.source)) {
			Device device = manager.getDevice(msg.source);

			// Acknowledgement received
			if (msg.type == 0x02) {
				synchronized (sent) {
					if (sent.containsKey(token) && device.getState() == DeviceState.PAIRING) {
						sent.remove(token);

						if (outputThreads.containsKey(msg.source)) {
							MessageThread thread = (MessageThread) outputThreads.get(msg.source);
							thread.interrupt();
						}
					}
				}
			} else if (msg.destination.equals("000000")) {
				logger.debug("Received broadcast {} from device {}", msg.number, msg.source);
				
			} else if (!msg.destination.equals("000000")) {
				logger.debug("Received message {} from device {}", msg.number, msg.source);
				// Acknowledge message
				sendAck(msg, device);
				device.setMessageNumber(msg.number);
				device.incMessageNumber();
			}

			Message cmd;
			synchronized (sent) {
				if (sent.containsKey(token)) {
					cmd = sent.get(token);
				} else {
					cmd = device.getLastMessage();
				}
				sent.remove(token);
			}
			device.parseMessage(msg, (CommandMessage) cmd);
		}
	}

	protected void sendAck(StatusMessage msg, Device device) {
		logger.debug("Sending acknoledgement {} to device {}", msg.number, msg.source);
		connection.sendFrame(new CommandMessage(device.getId(), id, (byte) 0x80, (byte) 0x02, "00")
				.getFrame(device, msg.number));
	}

	public void pushConfig(String address, String[] pushConfigData) throws HomeMaticException {
		numberOfPushConfigs = pushConfigData.length;
		for (String data : pushConfigData) {
			sendMessage(address, (byte) 0xA0, (byte) 0x01, data);
		}
	}

	public void sendMessage(String destination, byte flag, byte type, String data) throws HomeMaticException {
		if (!isReady()) {
			throw new HomeMaticException("Connection not yet established!");
		}
		CommandMessage cmdMessage = new CommandMessage(destination, id, flag, type, data);
		sendMessage(cmdMessage);
	}

	public void sendMessage(String destination, byte flag, byte type, byte[] data) throws HomeMaticException {
		if (!isReady()) {
			throw new HomeMaticException("Connection not yet established!");
		}
		CommandMessage cmdMessage = new CommandMessage(destination, id, flag, type, data);
		sendMessage(cmdMessage);
	}

	private void sendMessage(Message message) {
		String destination = message.getDestination();
		MessageThread thread = outputThreads.get(destination);
		if (thread == null) {
			thread = new MessageThread(destination);
			thread.setName("OGEMA-HomeMatic-CC1101-send-message");
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
		Iterator<MessageThread> it = outputThreads.values().iterator();
		while (it.hasNext()) {
			it.next().interrupt();
		}
	}

	private class MessageThread extends Thread {

		private static final int SEND_SLEEP = 2500;
		private static final int SEND_RETRIES = 256;

		private String destination;
		private int tries = 0;
		private int errors = 0;
		private int pushConfigCnt = 0;

		private MessageFifo<Message> unsent; // Messages waiting to be sent

		public MessageThread(String destination) {
			this.destination = destination;
			this.unsent = new MessageFifo<>(8);
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
							String data = cmd.data!=null?TypeConverter.toHexString(cmd.data):"";
							logger.debug("Sending message {} to device {}: {}", device.getMessageNumber(), destination, data);
							
							if (data.length() > 2 && data.substring(2).startsWith("0500000000")) { // Start_Config
								pushConfigCnt = 0;
							}

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
									data = cmd.data != null ? TypeConverter.toHexString(cmd.data) : "";
									if (data.length() > 2 && data.substring(2).startsWith("0500000000") || // Start_Config
										data.length() > 2 && data.substring(2).startsWith("08")	        || // Config
										data.length() > 2 && data.substring(2).startsWith("06")          ) { // End_Config
										pushConfigCnt++;
									}
									if (device.getState() == DeviceState.PAIRING && pushConfigCnt == numberOfPushConfigs) {
										device.setState(DeviceState.PAIRED);
										logger.info("Successfully paired device {} with address {}", device.getName(), destination);
										//TODO We want to use Configs later
//										device.getAllConfigs(); Not used yet
									}
								} else if (device.getState() == DeviceState.PAIRING) {
									// here we aren't sure that the device is no longer present. In case of configuration request,
									// the device wouldn't react, if the activation button is not pressed. Removing of devices
									// should be done actively by the user/administrator
									device.setState(DeviceState.UNKNOWN);
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

		private Object lock;

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
//						logger.debug("getReceivedFrame \"{}\" {}", new String(data), System.currentTimeMillis());
						int j = 0;
						for (int i = 0; i < data.length; i++) {
							if (i > 0 && data[i-1] == 13 && data[i] == 10) {  // look for Carriage Return
								byte[] nextData = new byte[i+1-j];
								System.arraycopy(data, j, nextData, 0, nextData.length);
								logger.debug("ReceivedFrame \"{}\" {}", new String(nextData), System.currentTimeMillis());
								handleMessage(nextData);
								j = i + 1;
							}
						}
						if (j == 0) { // no Carriage Return found, irregular message
							logger.warn("Invalid Message received: Reason no Carriage Return found.");
							logger.debug("Invalid Message received: Reason no Carriage Return found: \"{}\"", new String(data));
						}
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
				StatusMessage message;
				try {
					message = new StatusMessage(data);

					logger.debug("Received {} {} of type {} from device {}: {}",
							(message.destination.equals("000000") ? "broadcast" : "message"), message.number & 0x000000FF,
							message.type, message.source, TypeConverter.toHexString(message.data));
					if (message.data == null) {
						logger.warn("Invalid Message received: Reason Message contains no data.");
						logger.debug("Invalid Message received: Reason Message contains no data: \"{}\"", new String(data));
						return;
					}
				}
				catch (Exception e) {
					return;
				}
				
				if (!isReady()) {
					break;
				}
				if (message.type == 0x00 & manager.getPairing() != null) { // if pairing
					try {
						Device device = Device.create(manager.getDeviceDescriptor(), MessageHandler.this, message);
						if (manager.getPairing().equals("0000000000") | manager.getPairing().equals(device.getSerialPort())) {
							if (!manager.hasDevice(device.getId())) {
								manager.addDevice(device);
								logger.info("Received pairing request from device: {}", device.getId());
								
								device.startPairing();
							}
							else {
								device = manager.getDevice(device.getId());
								if (device.getState().equals(DeviceState.UNKNOWN)) {
									device.startPairing();
									
								} else if (device.getState().equals(DeviceState.PAIRED)) {
									device.activate();
								}
							}
						}
					} catch (HomeMaticException e) {
						// nothing to do here, because we break if connection is not ready (!isReady).
						// Messages are ignored during not ready state!
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
				logger.debug("Unknown message: " + TypeConverter.dumpHexString(data));
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
