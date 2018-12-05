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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.driver.homematic.HomeMaticConnectionException;
import org.ogema.driver.homematic.config.ConfigList;
import org.ogema.driver.homematic.config.ConfigListEntry;
import org.ogema.driver.homematic.config.ConfigListEntryValue;
import org.ogema.driver.homematic.config.ConfigLookups;
import org.ogema.driver.homematic.config.DeviceConfigs;
import org.ogema.driver.homematic.data.Value;
import org.ogema.driver.homematic.manager.devices.CO2Detector;
import org.ogema.driver.homematic.manager.devices.DeviceDescriptor;
import org.ogema.driver.homematic.manager.devices.MotionDetector;
import org.ogema.driver.homematic.manager.devices.PowerMeter;
import org.ogema.driver.homematic.manager.devices.Remote;
import org.ogema.driver.homematic.manager.devices.SmokeSensor;
import org.ogema.driver.homematic.manager.devices.SwitchPlug;
import org.ogema.driver.homematic.manager.devices.THSensor;
import org.ogema.driver.homematic.manager.devices.Thermostat;
import org.ogema.driver.homematic.manager.devices.ThreeStateSensor;
import org.ogema.driver.homematic.manager.messages.CommandMessage;
import org.ogema.driver.homematic.manager.messages.Message;
import org.ogema.driver.homematic.manager.messages.StatusMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a remote endpoint in a Homematic network.
 * 
 */
public abstract class Device {
	public final static Logger logger = LoggerFactory.getLogger(Device.class);

	private static final byte CONFIG_RESPONSE_TYPE1 = (byte) 0x02;
	private static final byte CONFIG_RESPONSE_TYPE2 = (byte) 0x03;

	public enum InitState {
		UNKNOWN, PAIRING, PAIRED
	}

	protected final String name;
	protected final String address;
	protected final String key;
	protected final String serial;

	// Message
	private Message messageLast;

	private int messageNum;

	DeviceConfigs configs;

	// States
	private InitState initState = InitState.UNKNOWN;

	protected DeviceDescriptor descriptor;
	protected MessageHandler messageHandler;
	
	public Map<Byte, DeviceCommand> deviceCommands;
	public Map<Short, DeviceAttribute> deviceAttributes;

	protected Device(DeviceDescriptor descriptor, MessageHandler messageHandler, 
			String address, String key, String serial) throws HomeMaticConnectionException {
		this.descriptor = descriptor;
		this.messageHandler = messageHandler;

		this.deviceAttributes = new HashMap<Short, DeviceAttribute>();
		this.deviceCommands = new HashMap<Byte, DeviceCommand>();
		this.address = address;
		this.key = key;
		this.serial = serial;
		this.name = descriptor.getName(this.key);
	}

	public static Device createDevice(DeviceDescriptor descriptor, MessageHandler messageHandler, 
			String address, String type, String serial) throws HomeMaticConnectionException {

		String key = descriptor.getType(type);
		switch (key) {
		case "THSensor":
			return new THSensor(descriptor, messageHandler, address, type, serial);
		case "threeStateSensor":
			boolean isDoorWindowSensor = type.equals("00B1");
			return new ThreeStateSensor(descriptor, messageHandler, address, type, serial, isDoorWindowSensor);
		case "thermostat":
			return new Thermostat(descriptor, messageHandler, address, type, serial);
		case "powerMeter":
			return new PowerMeter(descriptor, messageHandler, address, type, serial);
		case "smokeDetector":
			return new SmokeSensor(descriptor, messageHandler, address, type, serial);
		case "CO2Detector":
			return new CO2Detector(descriptor, messageHandler, address, type, serial);
		case "motionDetector":
			return new MotionDetector(descriptor, messageHandler, address, type, serial);
		case "switch":
			return new SwitchPlug(descriptor, messageHandler, address, type, serial);
		case "remote":
		case "pushbutton":
		case "swi":
			return new Remote(descriptor, messageHandler, address, type, serial);
		default:
			throw new RuntimeException("Type not supported: " + key);
		}
	}

	public static Device createPairedDevice(DeviceDescriptor descriptor, MessageHandler messageHandler, 
			String address, String type, String serial) throws HomeMaticConnectionException {

		Device device = createDevice(descriptor, messageHandler, address, type, serial);
		device.configureChannels();
		device.setInitState(InitState.PAIRED);
		return device;
	}

	public static Device createDevice(DeviceDescriptor descriptor, MessageHandler messageHandler, StatusMessage message) 
			throws HomeMaticConnectionException {
		return createDevice(descriptor, messageHandler, message.source, message.parseKey(), message.parseSerial());
	}

	public String getName() {
		return name;
	}

	public void init() throws HomeMaticConnectionException {
		this.init(true);
	}

	public void init(boolean channels) throws HomeMaticConnectionException {
		setInitState(InitState.PAIRING);
		pushConfig(getAddress(), "00", "00");
		
		if (channels) {
			configureChannels();
		}
	}

	private void pushConfig(String address, String channel, String list) throws HomeMaticConnectionException {
		String owner = messageHandler.getId();
		String pushConfigs = "0201" + "0A" + owner.charAt(0) + owner.charAt(1) + "0B" + owner.charAt(2) + owner.charAt(3)
				+ "0C" + owner.charAt(4) + owner.charAt(5);
		String[] pushConfigData = getPushConfigData(channel, list, pushConfigs);
		messageHandler.pushConfig(address, pushConfigData);
	}

	protected String[] getPushConfigData(String channel, String list, String pushConfigs) {
		String[] pushConfigData = new String[3];
		pushConfigData[0] = channel + "0500000000" + list;
		pushConfigData[1] = channel + "08" + pushConfigs;
		pushConfigData[2] = channel + "06";
		return pushConfigData;
	}
	
	protected abstract void configureChannels()  throws HomeMaticConnectionException;

	protected abstract void parseValue(StatusMessage msg);

	public abstract void parseMessage(StatusMessage msg, CommandMessage cmd, Device device);

	public abstract void channelChanged(byte identifier, Value value)  throws HomeMaticConnectionException;

	protected void parseConfig(StatusMessage response, CommandMessage cmd) {
		if (configs == null)
			configs = DeviceConfigs.getConfigs(getName());

		if (cmd == null)
			return; // should never happen
		// update message number for the case that the remote device has incremented it.
		setMessageNumber(response.number);
		byte contentType = response.data[0];
		// int length = (int) response.msg_data.length - 1;
		// int offset = 1;
		// get the relevant request data, list, channel and peer
		boolean peer = cmd.data[1] == 3; // 1. byte is 03 for peer configuration or 04 own configuration
		if (peer) {
			return;
		}
		
		int list;
		if (peer)
			list = 0;
		else
			list = cmd.data[6];
		int channel = cmd.data[0];
		// HMList listObj = HMList.allLists[list];

		switch (contentType) {
		case CONFIG_RESPONSE_TYPE1:
			getRegisterValues1(response.data, list);
			break;
		case CONFIG_RESPONSE_TYPE2:
			getRegisterValues2(response.data, list);
			break;
		default:
			logger.debug("configs not used content type {}", contentType);
			break;
		}
		parseRegisterEntry(list, channel);
		// TODO remoteDevice.removeSentMessage(cmd); // Garbage collection ist etwas aufwendiger, fehlende Information:
		// wann ist die Antwort auf ein bestimmtes Commando abgeschlossen? Deswegen ist ein immer wieder kehrender
		// Prozess erforderlich, der die alten CommandMessages entfernt.
	}

	private void getRegisterValues1(byte[] msg_data, int list) {
		Map<Integer, Integer> target = configs.getRegValues(list);
		int length = msg_data.length - 1;
		int offset = 1;
		while (length > 1) { // >1 because to each value belong two bytes
			int register = msg_data[offset++];
			int value = msg_data[offset++];
			logger.debug("configs1: register {}, value {}", register, value);
			target.put(register, value);
			length -= 2;
		}
	}

	private void getRegisterValues2(byte[] msg_data, int list) {
		int length = msg_data.length - 1;
		int offset = 1;
		int register = msg_data[offset++];
		length--;
		Map<Integer, Integer> target = configs.getRegValues(list);
		while (length > 0) { // > 0 because the start register is followed by a values each byte
			int value = msg_data[offset++];
			logger.debug("configs2: register {}, value {}", register, value);
			target.put(register++, value);
			length--;
		}
	}

	void parseRegisterEntry(int list, int channel) {
		ConfigList listObj = ConfigList.allLists[list];
		Map<Integer, Integer> registers = configs.getRegValues(list);
		// boolean pending;
		Set<Entry<Integer, Integer>> set = registers.entrySet();
		for (Entry<Integer, Integer> regEntry : set) {
			int register = regEntry.getKey();
			Object o = listObj.entriesByRegs.get(register);
			List<ConfigListEntryValue> valueList = getMatchingEntry(o);
			for (ConfigListEntryValue entryVal : valueList) {
				if (entryVal == null) {
					logger.debug("Not found reg: " + register + " in list: " + list);
				}
				else {
					logger.debug("Found reg: " + register + " in list: " + list);
					// int bytes = parseValue(entryVal, registers, register);
					entryVal.entry.channel = channel;
					logger.debug(entryVal.getDescription());
				}
			}
		}
	}

	private List<ConfigListEntryValue> getMatchingEntry(Object o) {
		ArrayList<ConfigListEntryValue> result = new ArrayList<ConfigListEntryValue>();
		if (o == null)
			return result;
		
		// String address = remoteDevice.getAddress();
		// HashMap<String, ListEntryValue> options = configs.getDevConfigs();
		if (!(o instanceof ConfigListEntry)) {
			@SuppressWarnings("unchecked")
			ArrayList<ConfigListEntry> list = (ArrayList<ConfigListEntry>) o;
			for (ConfigListEntry e : list) {
				ConfigListEntryValue entryVal = configs.getEntryValue(e);// getListEntryValue(e, options);
				if (entryVal != null) {
					result.add(entryVal);
				}
			}
		}
		else {
			ConfigListEntryValue entryVal = configs.getEntryValue(((ConfigListEntry) o));// getListEntryValue((ListEntry) o, options);
			if (entryVal != null) {
				result.add(entryVal);
			}
		}
		return result;
	}

	public int getMessageNumber() {
		return messageNum;
	}

	public int incMessageNumber() {
		return messageNum = (messageNum + 1) & 0x000000FF;
	}

	protected void setMessageNumber(int number) {
		int next = number & 0x000000FF;
		if (next > messageNum)
			messageNum = next;
	}

	public Message getLastMessage() {
		return messageLast;
	}

	public void pushConfig(int channel, int list, byte[] configs) throws HomeMaticConnectionException {
		// see HACK in 10_CUL_HM.pm, for the Thermostat HM-CC-RT-DN the channel 0 is a shadow of channel 4
		if (name.equals("HM-CC-RT-DN")) {
			if (list == 7 && channel == 4)
				channel = 0;
		}
		byte[] arr1 = { (byte) channel, 0x05, 0x00, 0x00, 0x00, 0x00, (byte) list };
		messageHandler.sendMessage(getAddress(), (byte) 0xA0, (byte) 0x01, arr1); // init config

		int length = configs.length;
		byte[] arr2 = new byte[length + 2];
		arr2[0] = (byte) channel;
		arr2[1] = 0x08;
		int i = 0;
		while (length > 0) {
			arr2[i + 2] = configs[i];
			length--;
			i++;
		}
		messageHandler.sendMessage(getAddress(), (byte) 0xA0, (byte) 0x01, arr2); // send config
		byte[] arr3 = { (byte) channel, 0x06 };
		messageHandler.sendMessage(getAddress(), (byte) 0xA0, (byte) 0x01, arr3); // terminate config
	}

	public String getAddress() {
		return address;
	}

	public String getKey() {
		return key;
	}

	public String getSerial() {
		return serial;
	}

	public InitState getInitState() {
		return initState;
	}

	public void setInitState(InitState initState) {
		this.initState = initState;
	}

	public void getAllConfigs() throws HomeMaticConnectionException { // 00040000000000: chNUM[1]| |peer[4]|lst[1]
		messageHandler.sendMessage(getAddress(), (byte) 0xA0, (byte) 0x01, "00040000000000"); // TODO: last byte should be
		// listnumber, first byte the channel number!
		String[] channels = descriptor.getChannels(key);
		String[] lists = descriptor.getLists(key);
		if (channels.length == 1)
			channels[0] = "autocreate:1:1";
		for (String chnstr : channels) {
			String[] channel = chnstr.split(":");
			if (lists.length != 0) {
				boolean pReq = false;
				for (String listEntry : lists) {
					boolean peerReq = false;
					boolean chnValid = false;
					String[] lstPart = listEntry.split(":");
					if (lstPart.length == 1) {
						chnValid = true;
						if (lstPart[0].equals("p") || lstPart[0].equals("3") || lstPart[0].equals("4"))
							peerReq = true;
					}
					else {
						String test = lstPart[1];
						String[] chnLst = test.split("\\.");
						for (String lchn : chnLst) {
							if (lchn.contains(channel[2]))
								chnValid = true;
							if (chnValid && lchn.contains("p"))
								peerReq = true;
						}
					}

					if (chnValid) {
						if (peerReq) {
							if (!pReq) {
								pReq = true;
								logger.debug("Send Peer Conf on channel " + channel[2]);
								messageHandler.sendMessage(getAddress(), (byte) 0xA0, (byte) 0x01, "0" + channel[2] + "03");
								try {
									Thread.sleep(1500);
								} catch (InterruptedException e) {
								}

							}
						}
						else {
							logger.debug("Send device Conf on channel " + channel[2] + " and list " + lstPart[0]);
							messageHandler.sendMessage(getAddress(), (byte) 0xA0, (byte) 0x01, "0" + channel[2] + "04000000000"
									+ lstPart[0]);
							try {
								Thread.sleep(1500); // TODO calculate an appropriate time to wait until the request is
								// responded.
							} catch (InterruptedException e) {
							}
						}
					}
				}
			}
		}
	}

	public String readConfigValue(String configName) {
		HashMap<String, ConfigListEntryValue> devconfigs = configs.getDeviceConfigs();
		ConfigListEntryValue entryV = devconfigs.get(configName);
		return entryV.getConfigValue(configName);
	}

	public String readConfigKey(String configName) {
		HashMap<String, ConfigListEntryValue> devconfigs = configs.getDeviceConfigs();
		ConfigListEntryValue entryV = devconfigs.get(configName);
		return entryV.getConfigKey(configName);
	}

	// PUSH CONFIGURATION
	public void writeConfig(String confName, String setting) throws HomeMaticConnectionException {
		HashMap<String, ConfigListEntryValue> devconfigs = configs.getDeviceConfigs();
		ConfigListEntryValue entryV = devconfigs.get(confName);
		ConfigListEntry entry = entryV.entry;
		int size = entry.size;
		int offset = entry.offsetBits;
		int pMask = ((1 << size) - 1);
		int nMask = ~(pMask << offset);
		int currentValue = entryV.getRegValue();
		String conv = entry.conversion;
		int regAddr = entry.register;

		int setValue = ConfigLookups.getValue2Setting(conv, setting, entry.factor);
		if (setValue == -1) {
			logger.error("Invalid setting %s", setting);
			return;
		}
		int bytesCnt = entry.getBytesCnt();
		byte[] configs = new byte[bytesCnt << 1];
		// write in big endian byte order
		setValue = setValue & pMask;
		setValue = setValue << offset;
		setValue = setValue | (currentValue & nMask);
		switch (bytesCnt) {
		case 1:
			configs[0] = (byte) (regAddr & 0xFF);
			configs[1] = (byte) (setValue & 0xFF);
			break;
		case 2:
			configs[0] = (byte) (regAddr & 0xFF);
			configs[1] = (byte) (setValue & 0xFF);
			configs[2] = (byte) (regAddr + 1 & 0xFF);
			configs[3] = (byte) (setValue >> 8 & 0xFF);
			break;
		case 3:
			configs[0] = (byte) (regAddr & 0xFF);
			configs[1] = (byte) (setValue & 0xFF);
			configs[2] = (byte) (regAddr + 1 & 0xFF);
			configs[3] = (byte) (setValue >> 8 & 0xFF);
			configs[4] = (byte) (regAddr + 2 & 0xFF);
			configs[5] = (byte) (setValue >> 16 & 0xFF);
			break;
		case 4:
			configs[0] = (byte) (regAddr & 0xFF);
			configs[1] = (byte) (setValue & 0xFF);
			configs[2] = (byte) (regAddr + 1 & 0xFF);
			configs[3] = (byte) (setValue >> 8 & 0xFF);
			configs[4] = (byte) (regAddr + 2 & 0xFF);
			configs[5] = (byte) (setValue >> 16 & 0xFF);
			configs[6] = (byte) (regAddr + 3 & 0xFF);
			configs[7] = (byte) (setValue >> 24 & 0xFF);
			break;
		default:
			break;
		}
		pushConfig(entry.channel, entry.list, configs);
	}

	public String listSupportedConfigs() {
		StringBuffer result = new StringBuffer();
		DeviceConfigs devconfig = configs;
		if (devconfig == null) {
			result.append(this.toString());
			result.append(" is not yet supported by configuration handling.");
		}
		else {
			HashMap<String, ConfigListEntryValue> configs = devconfig.getDeviceConfigs();
			Set<Entry<String, ConfigListEntryValue>> set = configs.entrySet();
			for (Entry<String, ConfigListEntryValue> entry : set) {
				result.append(entry.getKey());
				result.append('\t');
				ConfigListEntryValue entryVal = entry.getValue();
				if (entryVal == null)
					result.append(" : unexpectedly no value is provided by the device.");
				else
					result.append(entry.getValue().getDescription());
				result.append('\n');
			}
		}
		return result.toString();
	}
}
