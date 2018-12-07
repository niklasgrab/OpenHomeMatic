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
package org.ogema.driver.homematic.manager.devices;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceDescriptor {
	private final Logger logger = LoggerFactory.getLogger(DeviceDescriptor.class);

	private JSONObject jdata;
	private String json;
	private Iterator<String> Itr;
	private Map<String, JSONObject> deviceObjects = new HashMap<String, JSONObject>();

	public DeviceDescriptor() {
		InputStream is = getClass().getResourceAsStream("deviceTypes.json");
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			is.close();
			json = sb.toString();
		} catch (Exception e) {

		}
		try {
			jdata = new JSONObject(json);
			Iterator<String> keys = jdata.keys();
			Itr = keys;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		while (Itr.hasNext()) {
			String s = Itr.next();
			try {
				deviceObjects.put(s, jdata.getJSONObject(s));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
	}

	/*
	 * JSONObject: name st cyc rxt lst chn
	 */

	public String getName(String key) {
		String name = null;
		try {
			if (deviceObjects.containsKey(key)) {
				name = deviceObjects.get(key).getString("name");
			}
		} catch (JSONException e) {
			logger.error("Homematic device Type " + key + " unknown");
		}
		return name;
	}

	public String getType(String key) {
		String classType = null;
		try {
			if (deviceObjects.containsKey(key)) {
				classType = deviceObjects.get(key).getString("st");
			}
		} catch (JSONException e) {
			logger.error("Homematic device Type " + key + " unknown");
		}
		return classType;
	}

	public String[] getChannels(String key) {
		String chnstr = null;
		String[] channels = null;
		try {
			if (deviceObjects.containsKey(key)) {
				chnstr = deviceObjects.get(key).getString("chn");
				channels = chnstr.split(",");
			}
		} catch (JSONException e) {
			logger.error("Homematic device Type " + key + " unknown");
		}
		return channels;
	}

	public String[] getLists(String key) {
		String lststr = null;
		String[] lists = null;
		try {
			if (deviceObjects.containsKey(key)) {
				lststr = deviceObjects.get(key).getString("lst");
				lists = lststr.split(",");
			}
		} catch (JSONException e) {
			logger.error("Homematic device Type " + key + " unknown");
		}
		return lists;
	}

	// TODO: Implement all features

}
