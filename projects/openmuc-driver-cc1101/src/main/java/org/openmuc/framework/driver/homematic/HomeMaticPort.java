package org.openmuc.framework.driver.homematic;

import org.openmuc.framework.data.BooleanValue;
import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.FloatValue;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.StringValue;
import org.openmuc.framework.data.Value;

public class HomeMaticPort {

	public static org.ogema.driver.homematic.data.Value convert2CC1101(Value val) {
		String name = val.getClass().getSimpleName();
		org.ogema.driver.homematic.data.Value value = null;
		switch (name) {
			case "BooleanValue":
				value = convert2CC1101((BooleanValue)val);
				break;
			case "ByteArrayValue":
				value = convert2CC1101((ByteArrayValue)val);
				break;
			case "FloatValue":
				value = convert2CC1101((FloatValue)val);
				break;
			case "StringValue":
				value = convert2CC1101((StringValue)val);
				break;				
		}
		return value;
	}
	
	public static org.ogema.driver.homematic.data.BooleanValue convert2CC1101(BooleanValue val) {
		return new org.ogema.driver.homematic.data.BooleanValue(val.asBoolean());
	}
	
	public static org.ogema.driver.homematic.data.ByteArrayValue convert2CC1101(ByteArrayValue val) {
		return new org.ogema.driver.homematic.data.ByteArrayValue(val.asByteArray());
	}
	
	public static org.ogema.driver.homematic.data.FloatValue convert2CC1101(FloatValue val) {
		return new org.ogema.driver.homematic.data.FloatValue(val.asFloat());
	}
	
	public static org.ogema.driver.homematic.data.StringValue convert2CC1101(StringValue val) {
		return new org.ogema.driver.homematic.data.StringValue(val.asString());
	}
	
	public static Record convert2Openmuc(org.ogema.driver.homematic.data.Record record) {
		Value value = convert2Openmuc(record.getValue());
		Record rec = new Record(value, record.getTimestamp(), convert2Openmuc(record.getFlag()));
		return rec;
	}
	
	public static Value convert2Openmuc(org.ogema.driver.homematic.data.Value val) {
		if (val == null) return null;
		String name = val.getClass().getSimpleName();
		Value value = null;
		switch (name) {
			case "BooleanValue":
				value = convert2Openmuc((org.ogema.driver.homematic.data.BooleanValue)val);
				break;
			case "ByteArrayValue":
				value = convert2Openmuc((org.ogema.driver.homematic.data.ByteArrayValue)val);
				break;
			case "FloatValue":
				value = convert2Openmuc((org.ogema.driver.homematic.data.FloatValue)val);
				break;
			case "StringValue":
				value = convert2Openmuc((org.ogema.driver.homematic.data.StringValue)val);
				break;				
		}
		return value;
	}
		
	public static BooleanValue convert2Openmuc(org.ogema.driver.homematic.data.BooleanValue val) {
		return new BooleanValue(val.asBoolean());
	}

	public static ByteArrayValue convert2Openmuc(org.ogema.driver.homematic.data.ByteArrayValue val) {
		return new ByteArrayValue(val.asByteArray());
	}

	public static FloatValue convert2Openmuc(org.ogema.driver.homematic.data.FloatValue val) {
		return new FloatValue(val.asFloat());
	}
	
	public static StringValue convert2Openmuc(org.ogema.driver.homematic.data.StringValue val) {
		return new StringValue(val.asString());
	}
	
	public static Flag convert2Openmuc(org.ogema.driver.homematic.data.Flag flag) {
		return Flag.newFlag(flag.getCode());
	}
}
