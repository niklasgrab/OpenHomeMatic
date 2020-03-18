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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.ogema.driver.homematic.data.TypeConverter;
import org.ogema.driver.homematic.device.Device;

public class CommandMessage extends Message {

	private String id;
	private byte flag;
	private byte type;
	public byte[] data;

	public CommandMessage(String destination, String id, byte flag, byte type, byte[] data) {
		super(destination);
		this.id = id;
		this.flag = flag;
		this.type = type;
		this.data = data;
	}

	public CommandMessage(String destination, String id, byte flag, byte type, String data) {
		this(destination, id, flag, type, TypeConverter.hexStringToByteArray(data));
	}

	public String getId() {
		return id;
	}

	public byte getType() {
		return type;
	}

	@Override
	public byte[] getFrame(Device device) {
		return getFrame(device, device.getMessageNumber());
	}

	@Override
	public byte[] getFrame(Device device, int num) {
		ByteArrayOutputStream body = new ByteArrayOutputStream(1);
		TypeConverter.toHexString(num);
		try {
			body.write(TypeConverter.toHexString((byte)num).getBytes());
			body.write(TypeConverter.toHexString(flag).getBytes());
			body.write(TypeConverter.toHexString(type).getBytes());
			body.write(getId().getBytes());
			body.write(getDestination().getBytes());
			body.write(TypeConverter.toHexString(data).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		ByteBuffer message = ByteBuffer.allocate(64);
		message.put("As".getBytes());
		message.put(TypeConverter.toHexString((byte)(body.size()/2)).getBytes());
		message.put(body.toByteArray());

		byte[] frame = message.array();
		return frame;
	}
}
