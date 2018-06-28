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
package org.ogema.port;

import org.openmuc.framework.data.TypeConversionException;
import org.openmuc.framework.data.Value;

public class ObjectValue implements Value {

	private final Object value;

	public ObjectValue(Object value) {
		this.value = value;
	}

	@Override
	public float asFloat() throws TypeConversionException {
		throw new TypeConversionException("Cannot convert a generic object to a float");
	}

	@Override
	public double asDouble() throws TypeConversionException {
		throw new TypeConversionException("Cannot convert a generic object to a double");
	}

	@Override
	public int asInt() throws TypeConversionException {
		throw new TypeConversionException("Cannot convert a generic object to an integer");
	}

	@Override
	public long asLong() throws TypeConversionException {
		throw new TypeConversionException("Cannot convert a generic object to a long");
	}

	@Override
	public String asString() throws TypeConversionException {
		return value.toString();
	}

	@Override
	public byte[] asByteArray() throws TypeConversionException {
		throw new TypeConversionException("Cannot convert a generic object to a byte array.");
	}

	@Override
	public boolean asBoolean() throws TypeConversionException {
		throw new TypeConversionException("Cannot convert a generic object to a boolean.");
	}

	public Object getObjectValue() {
		return value;
	}

	@Override
	public Value clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException(
				"ObjectValue class has no sensible meaning of cloning since the type of object is arbitrary.");
	}

	@Override
	public byte asByte() throws TypeConversionException {
		throw new TypeConversionException("Cannot convert a generic object to a byte.");
	}

	@Override
	public short asShort() throws TypeConversionException {
		throw new TypeConversionException("Cannot convert a generic object to a short.");
	}

}
