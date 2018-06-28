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
package org.ogema.driver.homematic.manager.asksin.messages;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.ogema.driver.homematic.manager.asksin.LocalDevice;
import org.ogema.driver.homematic.manager.asksin.RemoteDevice;
import org.ogema.driver.homematic.tools.Converter;

/**
 * 
 * @author Godwin Burkhardt
 * 
 */
public class CmdMessage extends org.ogema.driver.homematic.manager.messages.CmdMessage {

	private byte flag;
	private byte type;

	private LocalDevice localDevice;

	public CmdMessage(LocalDevice localDevice, RemoteDevice rd, byte flag, byte type, String data) {
		super(localDevice, rd, flag, type, data);
		this.localDevice = localDevice;
		this.flag = flag;
		this.type = type;
	}

	public CmdMessage(LocalDevice localDevice, RemoteDevice rd, byte flag, byte type, byte[] data) {
		super(localDevice, rd, flag, type, data);
		this.localDevice = localDevice;
		this.flag = flag;
		this.type = type;
	}

	public byte[] getSerialFrame() {
		return getSerialFrame(this.num);
	}

	public byte[] getSerialFrame(int num) {
		this.num = num;
		ByteArrayOutputStream body = new ByteArrayOutputStream(1);
		Converter.toHexString(num);
		try {
			body.write(Converter.toHexString((byte)num).getBytes());
			body.write(Converter.toHexString(flag).getBytes());
			body.write(Converter.toHexString(type).getBytes());
			body.write(localDevice.getOwnerid().getBytes());
			body.write(dest.getBytes());
			body.write(Converter.toHexString(data).getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		ByteBuffer message = ByteBuffer.allocate(64);
		message.put("As".getBytes());
		message.put(Converter.toHexString((byte)(body.size()/2)).getBytes());
		message.put(body.toByteArray());

		byte[] frame = message.array();
		return frame;
	}
}
