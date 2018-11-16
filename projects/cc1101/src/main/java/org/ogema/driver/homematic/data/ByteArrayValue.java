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

package org.ogema.driver.homematic.data;

import java.util.Arrays;

/**
 * ByteArrayValue is not immutable.
 */
public class ByteArrayValue implements Value {

    private final byte[] value;

    /**
     * Create a new ByteArrayValue whose internal byte array will be a reference to the <code>value</code> passed to
     * this constructor. That means the passed byte array is not copied. Therefore you should not change the contents of
     * value after calling this constructor. If you want ByteArrayValue to internally store a copy of the passed value
     * then you should use the other constructor of this class instead.
     * 
     * @param value
     *            the byte array value.
     */
    public ByteArrayValue(byte[] value) {
        this.value = value;
    }

    /**
     * Creates a new ByteArrayValue copying the byte array passed if <code>copy</code> is true.
     * 
     * @param value
     *            the byte array value.
     * @param copy
     *            if true it will internally store a copy of value, else it will store a reference to value.
     */
    public ByteArrayValue(byte[] value, boolean copy) {
        if (copy) {
            this.value = value.clone();
        }
        else {
            this.value = value;
        }
    }

    @Override
    public double asDouble() {
        throw new TypeConversionException();
    }

    @Override
    public float asFloat() {
        throw new TypeConversionException();
    }

    @Override
    public long asLong() {
        throw new TypeConversionException();
    }

    @Override
    public int asInt() {
        throw new TypeConversionException();
    }

    @Override
    public short asShort() {
        throw new TypeConversionException();
    }

    @Override
    public byte asByte() {
        throw new TypeConversionException();
    }

    @Override
    public boolean asBoolean() {
        throw new TypeConversionException();
    }

    @Override
    public byte[] asByteArray() {
        return value;
    }

    @Override
    public String toString() {
        return Arrays.toString(this.value);
    }

    @Override
    public String asString() {
        return toString();
    }
}
