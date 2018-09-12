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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
public class OutputHandler {
	private final Logger logger = LoggerFactory.getLogger(OutputHandler.class);

	private static final int SEND_SLEEP = 2500;
	private static final int SEND_RETRIES = 4;

	private HomeMaticManager manager;

	private volatile Map<String, Message> sent = new LinkedHashMap<String, Message>(); // <Token>
	private volatile Map<String, MessageThread> threads = new ConcurrentHashMap<String, MessageThread>();

	public OutputHandler(HomeMaticManager manager) {
		this.manager = manager;
	}

	public void onReceivedMessage(StatusMessage msg) {
		String token = msg.source + msg.number;
		if (manager.hasDevice(msg.source)) {
			Device device = manager.getDevice(msg.source);
			
			// Acknowledgement received
			if (msg.type == 0x02) {
				synchronized(sent) {
					if (sent.containsKey(token) && device.getInitState() == InitState.PAIRING) {
						sent.remove(token);
						
						if (threads.containsKey(msg.source)) {
							MessageThread thread = (MessageThread) threads.get(msg.source);
							thread.interrupt();
						}
					}
				}
			}
			else if (!msg.destination.equals("000000")) {
				// Acknowledge message
				sendAck(msg, device);
			}
			
			CommandMessage cmd;
			synchronized(sent) {
				if (sent.containsKey(token)) {
					cmd = (CommandMessage) sent.get(token);
				}
				else {
					cmd = (CommandMessage) device.getLastMessage();
				}
				sent.remove(token);
			}
			device.parseMessage(msg, cmd);
		}
	}

	public void sendAck(StatusMessage msg, Device device) {
		logger.debug("Sending acknoledgement {} to device {}", msg.number, msg.source);
		manager.sendFrame(new CommandMessage(device, manager.getId(), (byte) 0x80, (byte) 0x02, "00").getFrame(msg.number));
	}

	public void sendMessage(Message message) {
		String destination = message.getDestination();
		MessageThread thread = threads.get(destination);
		if (thread == null) {
			thread = new MessageThread(destination);
			thread.setName("OGEMA-HomeMatic-CC1101-message");
			thread.start();
			
			threads.put(destination, thread);
		}
		thread.addMessage(message);
	}

	private class MessageThread extends Thread {

		private String destination;
		private volatile int tries = 0;
		private volatile int errors = 0;

		private volatile InputOutputFifo<Message> unsent; // Messages waiting to be sent

		public MessageThread(String destination) {
			this.destination = destination;
			this.unsent = new InputOutputFifo<>(8);
		}

		@Override
		public void run() {
			while (errors < 25) {
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
							entry =  this.unsent.get();
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
					if (device !=  null) {
						CommandMessage cmd = (CommandMessage) entry;
						String token = destination+cmd.getNumber();
						
						while (tries < SEND_RETRIES) {
							synchronized(sent) {
								if (sent.containsKey(token)) {
									sent.remove(token);
									token = destination+cmd.getNumber();
								}
								sent.put(token, cmd);
								if (logger.isTraceEnabled()) {
									logger.trace("Add message {} to await responses: {}", token, sent.keySet().toString());
								}
							}
							logger.debug("Sending message {} to device {}: {}", 
									cmd.getNumber(), destination, Converter.toHexString(cmd.data));
							
                            manager.sendFrame(cmd.getFrame());
                            
                            cmd.incNumber();
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
								}
								else if (device.getInitState() == InitState.PAIRING) {
									// here we aren't sure that the device is no longer present. In case of configuration request,
									// the device wouldn't react, if the activation button is not pressed. Removing of devices
									// should be done actively by the user/administrator
									device.setInitState(InitState.UNKNOWN);
									manager.getDevices().remove(destination);
									synchronized(sent) {
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
}
