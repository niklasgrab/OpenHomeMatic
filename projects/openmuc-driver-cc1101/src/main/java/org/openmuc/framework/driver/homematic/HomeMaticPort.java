package org.openmuc.framework.driver.homematic;

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;

public class HomeMaticPort {

	public static org.ogema.driver.homematic.data.Value encode(Value val) {
		String name = val.getClass().getSimpleName();
		org.ogema.driver.homematic.data.Value value = null;
		switch (name) {
			case "BooleanValue":
				value = enocde((BooleanValue)val);
				break;
			case "ByteArrayValue":
				value = encode((ByteArrayValue)val);
				break;
			case "FloatValue":
				value = encode((FloatValue)val);
				break;
			case "StringValue":
				value = encode((StringValue)val);
				break;				
		}
		return value;
	}

	public static org.ogema.driver.homematic.data.BooleanValue enocde(BooleanValue val) {
		return new org.ogema.driver.homematic.data.BooleanValue(val.asBoolean());
	}

	public static org.ogema.driver.homematic.data.ByteArrayValue encode(ByteArrayValue val) {
		return new org.ogema.driver.homematic.data.ByteArrayValue(val.asByteArray());
	}

	public static org.ogema.driver.homematic.data.FloatValue encode(FloatValue val) {
		return new org.ogema.driver.homematic.data.FloatValue(val.asFloat());
	}

	public static org.ogema.driver.homematic.data.StringValue encode(StringValue val) {
		return new org.ogema.driver.homematic.data.StringValue(val.asString());
	}

	public static Record decode(org.ogema.driver.homematic.data.TimeValue record) {
		Value value = decode(record.getValue());
		if (value == null) {
			return new Record(value, record.getTimestamp(), Flag.NO_VALUE_RECEIVED_YET);
		}
		return new Record(value, record.getTimestamp(), Flag.VALID);
	}

	public static Value decode(org.ogema.driver.homematic.data.Value val) {
		if (val == null) return null;
		String name = val.getClass().getSimpleName();
		Value value = null;
		switch (name) {
			case "BooleanValue":
				value = decode((org.ogema.driver.homematic.data.BooleanValue) val);
				break;
			case "ByteArrayValue":
				value = decode((org.ogema.driver.homematic.data.ByteArrayValue) val);
				break;
			case "FloatValue":
				value = decode((org.ogema.driver.homematic.data.FloatValue) val);
				break;
			case "StringValue":
				value = decode((org.ogema.driver.homematic.data.StringValue) val);
				break;				
		}
		return value;
	}

	public static BooleanValue decode(org.ogema.driver.homematic.data.BooleanValue val) {
		return new BooleanValue(val.asBoolean());
	}

	public static ByteArrayValue decode(org.ogema.driver.homematic.data.ByteArrayValue val) {
		return new ByteArrayValue(val.asByteArray());
	}

	public static FloatValue decode(org.ogema.driver.homematic.data.FloatValue val) {
		return new FloatValue(val.asFloat());
	}

	public static StringValue decode(org.ogema.driver.homematic.data.StringValue val) {
		return new StringValue(val.asString());
	}
}
