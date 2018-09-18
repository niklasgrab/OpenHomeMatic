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
package org.ogema.driver.homematic.manager.messages;

import org.ogema.driver.homematic.manager.Device;

public abstract class Message {

	protected Device device;

	protected Message(Device device) {
		this.device = device;
	}

	public String getDestination() {
		return device.getAddress();
	}

	public abstract byte[] getFrame();

	public abstract byte[] getFrame(int num);

	public int getNumber() {
		return device.getMessageNumber();
	}

	public int incNumber() {
		return device.incMessageNumber();
	}

	public Device getDevice() {
		return device;
	}
}
