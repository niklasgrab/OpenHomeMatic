/*
 * Copyright 2016-18 ISC Konstanz
 *
 * This file is part of OpenHomeMatic.
 * For more information visit https://github.com/isc-konstanz/OpenHomeMatic.
 *
 * OpenHomeMatic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenHomeMatic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenHomeMatic.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.ogema.driver.homematic.manager.asksin;

import java.util.ArrayList;
import java.util.List;

import org.ogema.driver.homematic.manager.MessageHandler;
import org.ogema.driver.homematic.manager.RemoteDevice.InitStates;
import org.ogema.driver.homematic.manager.asksin.messages.CmdMessage;
import org.ogema.driver.homematic.manager.asksin.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ogema.driver.homematic.manager.StatusMessage;

public class AsksinMessageHandler extends MessageHandler {
	private final Logger logger = LoggerFactory.getLogger(AsksinMessageHandler.class);

	private volatile List<String> sentMessageSerialAwaitingResponse = new ArrayList<String>(); // <Token>

	private int pairing = 0;

	public AsksinMessageHandler(LocalDevice device) {
		super(device);
	}

//TODO ugly ProtocolType and Connection type is mixed!??  Concerns whole class. 	
	
	@Override
	public void messageReceived(StatusMessage msg) {
		RemoteDevice device = (RemoteDevice) localDevice.getDevices().get(msg.source);
		String token = msg.source + msg.msg_num;
		logger.debug("Received ?-token: " + token);
		// msg_type 0x02 is receive from pairing requests or command
		if (msg.msg_type == 0x02) {
			if (sentMessageSerialAwaitingResponse.contains(token)) {
				if (runningThreads.containsKey(msg.source) && pairing == device.getPairing() ) {
					SendThreadSerial sendThread = (SendThreadSerial) runningThreads.get(msg.source);
					if (device.getInitState() != InitStates.PAIRED) {
						device.augmentPairing();
					}
					sentMessageSerialAwaitingResponse.remove(token);
					logger.info("sentMessageAwaitingResponse removed " + msg.rtoken);
					sendThread.interrupt();
					logger.info("Thread has been notified");				
				}
			}
			else {
				logger.debug("sentMessageSerialAwaitingResponse contains not the token " + token);
			}
		}
		else {
			// msg_type 0x10 is receive Status from 010E request. In this case remove the token 
			// to prevent sending the same message again
			if (sentMessageSerialAwaitingResponse.contains(token)) {
				if (runningThreads.containsKey(msg.source)) {
					SendThreadSerial sendThread = (SendThreadSerial) runningThreads.get(msg.source);
					sentMessageSerialAwaitingResponse.remove(token);
					logger.info("sentMessageAwaitingResponse removed !02" + msg.rtoken);
					sendThread.interrupt();
					logger.info("Thread has been notified !02");
				}
				else {
					logger.info("Send Thread for " + msg.source + " not found!");
				}
			}
			CmdMessage cmd;
			String key;
			synchronized (sentCommands) {
				key = msg.source + "" + device.sentMsgNum;
				cmd = (CmdMessage) sentCommands.get(key);
			}
			logger.debug("Received message assigned to " + key);
			device.parseMsg(msg, cmd);
		}
	}
	
	@Override
	public void sendMessage(org.ogema.driver.homematic.manager.messages.Message message) {
		String dest = message.getDest();
		SendThread sendThread = runningThreads.get(dest);
		if (sendThread == null) {
			sendThread = new SendThreadSerial(dest);
			runningThreads.put(dest, sendThread);
			sendThread.start();
		}
		sendThread.addMessage(message);
	}
	
	public class SendThreadSerial extends SendThread {

		public SendThreadSerial(String dest) {
			super(dest);
			this.setName("Homematic-SendThread-Serial" + dest);
		}

		@Override
		public void run() {
			while (errorCounter < 25) {
				try {
					org.ogema.driver.homematic.manager.messages.Message entry = null;
					logger.debug("Try: " + tries);
					synchronized (unsentMessageQueue) {
						// entry = this.unsentMessageQueue.remove(getSmallestKey());
						entry = (org.ogema.driver.homematic.manager.messages.Message) this.unsentMessageQueue.get();
						if (entry == null) {
							try {
								unsentMessageQueue.wait();
							} catch (InterruptedException e) {
								logger.debug("Waiting SendThread interrupted");
							}
							// entry = this.unsentMessageQueue.get(getSmallestKey());
							entry = (org.ogema.driver.homematic.manager.messages.Message) this.unsentMessageQueue.get();
							if (entry == null)
								continue;
						}
					}
					String token = entry.getDest();
					if (entry instanceof CmdMessage) {
						int num = entry.refreshMsg_num();
						token += num;
						sentMessageSerialAwaitingResponse.add(token);
						logger.debug("sentMessageSerialAwaitingResponse added " + token);
						String key = entry.getDest() + "" + num;
						synchronized (sentCommands) {
							entry.getDevice().sentMsgNum = num;
							((CmdMessage) entry).sentNum = num;
							sentCommands.put(key, (CmdMessage) entry);
						}
						System.out.println("Sent command registered with  key: " + key);
					}
					else {
						// should not happen
						logger.debug("message is not instance of HMCmdMessage, but " + entry.getClass());						
					}
					RemoteDevice device = (RemoteDevice) localDevice.getDevices().get(dest);
					if (device !=  null) {
					pairing = device.getPairing();
					while (tries < HM_SENT_RETRIES) {
						if (sentMessageSerialAwaitingResponse.contains(token)) {
							if (entry instanceof Message) {
								localDevice.sendFrame(((Message)entry).getSerialFrame());
							}
							else if (entry instanceof CmdMessage) {
								localDevice.sendFrame(((CmdMessage)entry).getSerialFrame());
							}
							else {
								localDevice.sendFrame(entry.getFrame());
							}
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								logger.debug("Sleeping SendThread interrupted");
								break;
							}
							logger.debug(
									String.format("Response from %s for the message took to long ...", dest));
							tries++;
						}
						else {
							logger.debug("unsentMessageQueue removed " + token);
							break;
						}
					}
					if (!sentMessageSerialAwaitingResponse.contains(token) && tries <= HM_SENT_RETRIES) {
						logger.debug("Message sent to device " + dest);
						if (device.getInitState() == InitStates.PAIRING) {
							if (device.getPairing() == 4) {
								device.setInitState(InitStates.PAIRED);
								logger.debug("Device " + dest + " paired");
								device.setPairing(0);
							}
						}
					}
					else if (device.getPairing() > 0 && device.getPairing() <= 3) {
						// here we aren't sure that the device is no
						// longer present. In case of configuration
						// request,
						// the device wouldn't react, if the activation button is not pressed. Removing of devices
						// should be done actively by the user/administrator
						device.setInitState(InitStates.UNKNOWN);
						localDevice.getDevices().remove(device.getAddress());
						logger.warn("Device " + dest + " removed!");
					}
					if (device.getPairing() == 4) {
						device.setPairing(0);
					}
					}
					tries = 0;
					errorCounter = 0;
				} catch (Exception e) {
					logger.error("Error in Homematic message handler thread", e);
					errorCounter++;
				}
			}
		}
	}
}
