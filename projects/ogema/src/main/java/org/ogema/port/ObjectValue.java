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
