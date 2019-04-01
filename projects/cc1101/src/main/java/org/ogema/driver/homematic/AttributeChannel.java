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
package org.ogema.driver.homematic;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.ogema.driver.homematic.data.ByteArrayValue;
import org.ogema.driver.homematic.data.ObjectValue;
import org.ogema.driver.homematic.data.TimeValue;
import org.ogema.driver.homematic.data.TypeConverter;
import org.ogema.driver.homematic.data.UpdateListener;
import org.ogema.driver.homematic.data.Value;
import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.DeviceAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AttributeChannel extends HomeMaticChannel {
	private final Logger logger = LoggerFactory.getLogger(AttributeChannel.class);

	private DeviceAttribute deviceAttribute;
	private final byte[] emptyMessagePayload = new byte[0];
	private UpdateListener updateListener;
	private ByteBuffer messagePayloadBuffer;
	private Map<Short, TimeValue> recordMap = new HashMap<Short, TimeValue>();
	private ArrayList<DeviceAttribute> deviceAttributes = new ArrayList<DeviceAttribute>();
	private final boolean multipleAttributes;

	protected AttributeChannel(String address, String[] configs, Device device) {
		super(address);
		
		if (configs.length <= 2) { // Only one attribute
			multipleAttributes = false;
			byte[] attributeIdArray = TypeConverter.hexToBytes(configs[1]);
			short attributeId = (short) ((short) (attributeIdArray[0] << 8) & 0xff00);
			attributeId |= attributeIdArray[1] & 0x00ff;
			
			deviceAttribute = device.deviceAttributes.get(attributeId);
		}
		else { // Multiple attributes
			multipleAttributes = true;
			byte[] messagePayload = new byte[(configs.length - 3) * 2]; // Store all attributeIds starting from the
			// second, *2 because each id consists
			// of 2 bytes
			messagePayloadBuffer = ByteBuffer.wrap(messagePayload);

			for (int i = 1; i < configs.length; ++i) {
				byte[] attributeIdArray = TypeConverter.hexToBytes(configs[i]);
				short attributeId = (short) ((short) (attributeIdArray[0] << 8) & 0xff00);
				attributeId |= attributeIdArray[1] & 0x00ff;
				if (i > 1) // Skip the first attribute ID because it is implied
					// when sending via that attribute
					messagePayloadBuffer.putShort(Short.reverseBytes(attributeId));
				logger.debug(" " + Integer.toHexString(attributeId));
				
				deviceAttributes.add(device.deviceAttributes.get(attributeId));
			}
		}
	}

	@Override
	public TimeValue readRecord() throws IOException, UnsupportedOperationException {
		if (multipleAttributes) {
			for (DeviceAttribute deviceAttribute : deviceAttributes) { // Retrieve the values from all attributes
				TimeValue record = new TimeValue(deviceAttribute.getValue(), deviceAttribute.getValueTimestamp());
				recordMap.put(deviceAttribute.getShortId(), record);
			}
			Value value = new ObjectValue(recordMap);
			return new TimeValue(value, deviceAttribute.getValueTimestamp()); // TODO use average for Quality?
		}
		else {
			return new TimeValue(deviceAttribute.getValue(), deviceAttribute.getValueTimestamp());
		}
	}

	/**
	 * The value has to be a ByteArrayValue in Little Endian Byte order
	 */
	@Override
	public void writeValue(Value value) throws IOException, UnsupportedOperationException {
		// NYI!
		if (multipleAttributes) {
			throw new UnsupportedOperationException(); // TODO implement this method
		}
		else {
			if (deviceAttribute.readOnly() || !(value instanceof ByteArrayValue))
				throw new UnsupportedOperationException();
			byte[] messagePayload = value.asByteArray();
			if (messagePayload == null)
				messagePayload = emptyMessagePayload;
		}
	}

	@Override
	public void setEventListener(UpdateListener listener) throws IOException,
			UnsupportedOperationException {
		if (multipleAttributes) {
			throw new UnsupportedOperationException(); // TODO implement this method
		}
		else {
			deviceAttribute.setChannel(this);
			deviceAttribute.setListener(true);
			updateListener = listener;
		}
	}

	@Override
	public void removeUpdateListener() {
		if (multipleAttributes) {
			throw new UnsupportedOperationException(); // TODO implement this method
		}
		else {
			deviceAttribute.setListener(false);
			updateListener = null;
		}
	}

	public void updateListener() {
		// TODO adjust quality
		if (multipleAttributes) {
			throw new UnsupportedOperationException(); // TODO implement this method
		}
		else {
			TimeValue record = new TimeValue(deviceAttribute.getValue(), deviceAttribute.getValueTimestamp());
			updateListener.valueChanged(record, address);
		}
	}
}
