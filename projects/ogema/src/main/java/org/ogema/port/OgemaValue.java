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

import org.ogema.driver.homematic.manager.ValueType;
import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.IntValue;
import org.openmuc.framework.data.LongValue;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;

public class OgemaValue {

	public static Value decode(org.ogema.core.channelmanager.measurements.Value in) {
		if (in instanceof org.ogema.core.channelmanager.measurements.BooleanValue) {
			return new BooleanValue(in.getBooleanValue());
		}
		else if (in instanceof org.ogema.core.channelmanager.measurements.ByteArrayValue) {
			return new ByteArrayValue(in.getByteArrayValue());
		}
		else if (in instanceof org.ogema.core.channelmanager.measurements.DoubleValue) {
			return new DoubleValue(in.getDoubleValue());
		}
		else if (in instanceof org.ogema.core.channelmanager.measurements.FloatValue) {
			return new FloatValue(in.getFloatValue());
		}
		else if (in instanceof org.ogema.core.channelmanager.measurements.IntegerValue) {
			return new IntValue(in.getIntegerValue());
		}
		else if (in instanceof org.ogema.core.channelmanager.measurements.LongValue) {
			return new LongValue(in.getLongValue());
		}
		else if (in instanceof org.ogema.core.channelmanager.measurements.ObjectValue) {
			return new ObjectValue(in.getObjectValue());
		}
		else if (in instanceof org.ogema.core.channelmanager.measurements.StringValue) {
			return new StringValue(in.getStringValue());
		}
		
		return null;
	}
	
	public static org.ogema.core.channelmanager.measurements.Value encode(Value in) {
		if (in instanceof BooleanValue) {
			return new org.ogema.core.channelmanager.measurements.BooleanValue(in.asBoolean());
		}
		else if (in instanceof ByteArrayValue) {
			return new org.ogema.core.channelmanager.measurements.ByteArrayValue(in.asByteArray());
		}
		else if (in instanceof DoubleValue) {
			return new org.ogema.core.channelmanager.measurements.DoubleValue(in.asDouble());
		}
		else if (in instanceof FloatValue) {
			return new org.ogema.core.channelmanager.measurements.FloatValue(in.asFloat());
		}
		else if (in instanceof IntValue) {
			return new org.ogema.core.channelmanager.measurements.IntegerValue(in.asInt());
		}
		else if (in instanceof LongValue) {
			return new org.ogema.core.channelmanager.measurements.LongValue(in.asLong());
		}
		else if (in instanceof ObjectValue) {
			return new org.ogema.core.channelmanager.measurements.ObjectValue(((ObjectValue)in).getObjectValue());
		}
		else if (in instanceof StringValue) {
			return new org.ogema.core.channelmanager.measurements.StringValue(in.asString());
		}
		
		return null;
	}
	
	public static org.openmuc.framework.data.ValueType encodeValueType(ValueType valuetype) {
		if (valuetype.equals(ValueType.BOOLEAN)) {
			return org.openmuc.framework.data.ValueType.BOOLEAN;
		}
		else if (valuetype.equals(ValueType.BYTE)) {
			return org.openmuc.framework.data.ValueType.BYTE;
		}
		else if (valuetype.equals(ValueType.BYTE_ARRAY)) {
			return org.openmuc.framework.data.ValueType.BYTE_ARRAY;
		}
		else if (valuetype.equals(ValueType.DOUBLE)) {
			return org.openmuc.framework.data.ValueType.DOUBLE;
		}
		else if (valuetype.equals(ValueType.FLOAT)) {
			return org.openmuc.framework.data.ValueType.FLOAT;
		}
		else if (valuetype.equals(ValueType.INTEGER)) {
			return org.openmuc.framework.data.ValueType.INTEGER;
		}
		else if (valuetype.equals(ValueType.LONG)) {
			return org.openmuc.framework.data.ValueType.LONG;
		}
		else if (valuetype.equals(ValueType.SHORT)) {
			return org.openmuc.framework.data.ValueType.SHORT;
		}
		else if (valuetype.equals(ValueType.STRING)) {
			return org.openmuc.framework.data.ValueType.STRING;
		}
		else {
			return null;
		}
	}

}
