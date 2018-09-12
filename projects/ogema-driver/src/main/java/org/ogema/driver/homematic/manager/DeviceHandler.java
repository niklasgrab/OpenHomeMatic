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

import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.driver.homematic.config.DeviceConfigs;
import org.ogema.driver.homematic.config.ConfigList;
import org.ogema.driver.homematic.config.ConfigListEntry;
import org.ogema.driver.homematic.config.ConfigListEntryValue;
import org.ogema.driver.homematic.manager.messages.CommandMessage;
import org.ogema.driver.homematic.manager.messages.StatusMessage;

public abstract class DeviceHandler {

	private static final byte CONFIG_RESPONSE_TYPE1 = (byte) 0x02;
	private static final byte CONFIG_RESPONSE_TYPE2 = (byte) 0x03;

	protected Device device;

	public Map<Byte, DeviceCommand> deviceCommands;
	public Map<Short, DeviceAttribute> deviceAttributes;

	DeviceConfigs configs;

	public DeviceHandler(Device device) {
		this.device = device;

		deviceCommands = new HashMap<Byte, DeviceCommand>();
		deviceAttributes = new HashMap<Short, DeviceAttribute>();
	}

	protected abstract void configureChannels();

	public abstract void parseValue(StatusMessage msg);

	public abstract void parseMessage(StatusMessage msg, CommandMessage cmd);

	public abstract void channelChanged(byte identifier, Value value);

	public void parseConfig(StatusMessage response, CommandMessage cmd) {
		if (configs == null)
			configs = DeviceConfigs.getConfigs(device.getName());

		if (cmd == null)
			return; // should never happen
		// update message number for the case that the remote device has incremented it.
		device.setMessageNumber(response.number);
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
				if (entryVal != null) {
					entryVal.entry.channel = channel;
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

}
