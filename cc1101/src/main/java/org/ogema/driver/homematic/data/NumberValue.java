/*
 * Copyright 2016-20 ISC Konstanz
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

abstract class NumberValue implements Value {
    private final Number value;

    public NumberValue(Number value) {
        this.value = value;
    }

    @Override
    public double asDouble() {
        return this.value.doubleValue();
    }

    @Override
    public float asFloat() {
        return this.value.floatValue();
    }

    @Override
    public long asLong() {
        return this.value.longValue();
    }

    @Override
    public int asInt() {
        return this.value.intValue();
    }

    @Override
    public short asShort() {
        return this.value.shortValue();
    }

    @Override
    public byte asByte() {
        return this.value.byteValue();
    }

    @Override
    public boolean asBoolean() {
        return this.value.doubleValue() != 0.0;
    }

    @Override
    public byte[] asByteArray() {
        return null;
    }

    @Override
    public String asString() {
        return this.value.toString();
    }

    @Override
    public String toString() {
        return asString();
    }

}
