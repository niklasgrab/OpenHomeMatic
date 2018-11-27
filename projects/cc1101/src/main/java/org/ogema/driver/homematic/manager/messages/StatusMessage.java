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

import java.util.Arrays;

import org.ogema.driver.homematic.tools.Converter;

public class StatusMessage {

	public String source;
	public String destination;

	public long length = 0;
	public int number = 0;
	public byte flag = 0;
	public byte type = 0;
	public byte[] data = null;
	public byte[] msg = null;

	public boolean partyMode = false;
	protected boolean isEmpty = false;

	public StatusMessage() {
		isEmpty = true;
	}

	public StatusMessage(byte[] data) {
		switch (data[0]) {
		case 'A':
			byte[] tmp = parseAscii(Arrays.copyOfRange(data, 1, data.length));
			data = new byte[tmp.length+1];
			data[0] = 0;
			
			System.arraycopy(tmp, 0, data, 1, tmp.length);
		case 'a':
			parse(data, 1);
			break;
		default:
			isEmpty = true;
			break;
		}
	}

	void parse(byte[] data, int start) {
		int startAll = start;
		
		length = Converter.toLong(data[start++]);
		number = Converter.toInt(data[start++]);
		flag = data[start++];
		type = data[start++];
		
		source = Converter.toHexString(data, start, 3);
		start += 3;
		
		destination = Converter.toHexString(data, start, 3);
		start += 3;
		
		if (this.length > 9)
			this.data = Arrays.copyOfRange(data, start, (int) (2 + this.length));
		msg = Arrays.copyOfRange(data, startAll, (int) (2 + this.length));

		if (source.equals(destination))
			partyMode = true;
	}

	public byte[] parseAscii(byte[] data) {
		byte[] retVal = new byte[data.length/2];
		for (int i = 0; i < data.length; i+=2) {
			retVal[i/2] = (byte) ((Character.digit(data[i], 16) << 4)
					+ Character.digit(data[i+1], 16));
		}
		
		return retVal;
	}

	public String parseKey() {
		return Converter.toHexString(data, 1, 2);
	}

	public String parseSerial() {
		return new String(Arrays.copyOfRange(data, 3, 13));
	}

}
