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
package org.ogema.driver.homematic.config;

import java.util.ArrayList;
import java.util.HashMap;

public class ConfigList {

	public static ConfigList[] allLists;

	public static HashMap<String, ConfigListEntry> entriesByName;

	static {
		entriesByName = new HashMap<String, ConfigListEntry>();
		allLists = new ConfigList[8];
		allLists[0] = new ConfigList0();
		allLists[1] = new ConfigList1();
		allLists[2] = null;
		allLists[3] = new ConfigList3();
		allLists[4] = new ConfigList4();
		allLists[5] = new ConfigList5();
		allLists[6] = new ConfigList6();
		allLists[7] = new ConfigList7();
	}

	public HashMap<Integer, Object> entriesByRegs;

	ConfigList() {
		entriesByRegs = new HashMap<Integer, Object>();
	}

	@SuppressWarnings("unchecked")
	ConfigListEntry nl(int list, int register, int offsetBits, String name, int sizeB, float min, float max,
			String conversion, float factor, String unit, boolean inReading, String help) {
		ConfigListEntry e = new ConfigListEntry(list, name, register, offsetBits, sizeB, min, max, conversion, factor, unit,
				inReading, help);
		// Check if an entry exists for this register
		Object o = entriesByRegs.get(register);
		if (o == null) // The register was not yet present in any entry, put it as single entry object
			entriesByRegs.put(register, e);
		else if (o instanceof ArrayList<?>) // The register was already present in many entries, add the new entry to
			// the list
			((ArrayList<ConfigListEntry>) o).add(e);
		else if (o instanceof ConfigListEntry) { // The register was present as single entry, replace by a list containing the
			// new and the old entry
			ArrayList<ConfigListEntry> lst = new ArrayList<ConfigListEntry>();
			lst.add((ConfigListEntry) o);
			lst.add(e);
			entriesByRegs.put(register, lst);
		}

		o = entriesByName.get(name);
		if (o == null)
			entriesByName.put(name, e);
		return e;
	}

	public static ConfigListEntry getEntryByName(String name) {
		ConfigListEntry entry = entriesByName.get(name);
		return entry;
	}

}
