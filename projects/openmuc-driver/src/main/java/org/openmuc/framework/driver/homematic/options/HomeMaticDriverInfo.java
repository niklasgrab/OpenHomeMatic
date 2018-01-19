/*
 * Copyright 2017-18 ISC Konstanz
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
package org.openmuc.framework.driver.homematic.options;

import java.util.HashMap;
import java.util.Map;

import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.DriverInfo;
import org.openmuc.framework.config.options.Preferences;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;

public class HomeMaticDriverInfo extends DriverInfo {

	private final static HomeMaticDriverInfo info = new HomeMaticDriverInfo();

	private final Map<String, HomeMaticChannelPreferences> channels = new HashMap<String, HomeMaticChannelPreferences>();

	private HomeMaticDriverInfo() {
		super(HomeMaticDriverInfo.class.getResourceAsStream("options.xml"));
	}

	public static HomeMaticDriverInfo getInfo() {
		return info;
	}

	public HomeMaticDevicePreferences getDevicePreferences(String addressStr, String settingsStr) throws ArgumentSyntaxException {
		Preferences address = parseDeviceAddress(addressStr);
		
		return new HomeMaticDevicePreferences(address);
	}

	public HomeMaticChannelPreferences getChannelPreferences(ChannelValueContainer container) throws ArgumentSyntaxException {
		String address = container.getChannelAddress();
		String settings = container.getChannelSettings();
		
		return new HomeMaticChannelPreferences(address, settings, parseChannelSettings(settings));
	}

	public HomeMaticChannelPreferences getChannelPreferences(ChannelRecordContainer container) throws ArgumentSyntaxException {
		String id = container.getChannel().getId();
		String address = container.getChannelAddress();
		String settings = container.getChannelSettings();
		if (channels.containsKey(id)) {
			HomeMaticChannelPreferences prefs = channels.get(id);
			if (prefs.equals(address, settings)) {
				return prefs;
			}
		}
		return new HomeMaticChannelPreferences(address, settings, parseChannelSettings(settings));
	}

}
