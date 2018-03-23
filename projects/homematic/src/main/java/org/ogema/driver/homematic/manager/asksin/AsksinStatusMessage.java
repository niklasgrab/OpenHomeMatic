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
package org.ogema.driver.homematic.manager.asksin;

import java.util.Arrays;

import org.ogema.driver.homematic.manager.StatusMessage;
import org.ogema.driver.homematic.tools.Converter;
import org.slf4j.Logger;

/**
 * 
 * @author Godwin Burkhardt
 * 
 */
public class AsksinStatusMessage extends StatusMessage {

	private final Logger logger = org.slf4j.LoggerFactory.getLogger("homematic-driver");

	public AsksinStatusMessage() {
		super();
	}
	
	public AsksinStatusMessage(byte[] data) {
		super(data);
		type = data[0];
		switch (type) {
		case 'A':
			byte[] tmp = hexAsciiByte2binaryByte(Arrays.copyOfRange(data, 1, data.length));
			byte[] newData = new byte[tmp.length+1];
			newData[0] = 0;
			System.arraycopy(tmp, 0, newData, 1, tmp.length);
			parseMsgAll(newData, 1);
			break;
		case 'a':
			parseMsgAll(data, 1);
			break;			
		}
	}

	void parseMsgAll(byte[] data, int pos) {
		int allPos = pos;
		msg_len = Converter.toLong(data[pos++]);
		msg_num = Converter.toInt(data[pos++]);
		msg_flag = data[pos++];
		msg_type = data[pos++];
		source = Converter.toHexString(data, pos, 3);
		pos += 3;
		destination = Converter.toHexString(data, pos, 3);
		pos += 3;
		if (this.msg_len > 9)
			msg_data = Arrays.copyOfRange(data, pos, (int) (2 + this.msg_len));
		msg_all = Arrays.copyOfRange(data, allPos, (int) (2 + this.msg_len));

		if (source.equals(destination))
			partyMode = true;		

		logger.debug("Msg Len: " + msg_len +  ", Num: " + msg_num + ", Flag: " + msg_flag + ", type: " + msg_type + 
				", src: " + source + ", dest: " + destination + ", data: " + Converter.dumpHexString(msg_data));
		
	}
	
	public static byte[] hexAsciiByte2binaryByte(byte[] data) {
		byte[] retVal = new byte[data.length/2];
		for (int i = 0; i < data.length; i+=2) {
			retVal[i/2] = (byte) ((Character.digit(data[i], 16) << 4)
                    + Character.digit(data[i+1], 16));
		}
		
		return retVal;
	}
}
