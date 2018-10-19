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
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.openmuc.framework.data.ByteArrayValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.ogema.driver.homematic.data.ObjectValue;
import org.ogema.driver.homematic.manager.Device;
import org.ogema.driver.homematic.manager.DeviceAttribute;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AttributeChannel extends HomeMaticChannel {
	private final Logger logger = LoggerFactory.getLogger(AttributeChannel.class);

	private DeviceAttribute deviceAttribute;
	private final byte[] emptyMessagePayload = new byte[0];
	private RecordsReceivedListener recordReceivedListener;
	private List<ChannelRecordContainer> recordContainerList = new ArrayList<ChannelRecordContainer>();
	private ByteBuffer messagePayloadBuffer;
	private Map<Short, Record> recordMap = new HashMap<Short, Record>();
	private ArrayList<DeviceAttribute> deviceAttributes = new ArrayList<DeviceAttribute>();
	private final boolean multipleAttributes;

	public AttributeChannel(String address, String[] configs, Device device) {
		super(address);
		
		if (configs.length <= 2) { // Only one attribute
			multipleAttributes = false;
			byte[] attributeIdArray = DatatypeConverter.parseHexBinary(configs[1]);
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
				byte[] attributeIdArray = DatatypeConverter.parseHexBinary(configs[i]);
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
	public Record readRecord() throws IOException, UnsupportedOperationException {
		if (multipleAttributes) {
			for (DeviceAttribute deviceAttribute : deviceAttributes) { // Retrieve the values from all attributes
				recordMap.put(deviceAttribute.getShortId(), new Record(deviceAttribute.getValue(),
						deviceAttribute.getValueTimestamp(), Flag.VALID));
			}
			Value value = new ObjectValue(recordMap);
			return new Record(value, deviceAttribute.getValueTimestamp(), Flag.VALID); // TODO use average for Quality?
		}
		else {
			return new Record(deviceAttribute.getValue(), deviceAttribute.getValueTimestamp(), Flag.VALID);
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
	public void setEventListener(ChannelRecordContainer container, RecordsReceivedListener listener) throws IOException,
			UnsupportedOperationException {
		if (multipleAttributes) {
			throw new UnsupportedOperationException(); // TODO implement this method
		}
		else {
			deviceAttribute.setChannel(this);
			deviceAttribute.setListener(true);
			recordReceivedListener = listener;
			recordContainerList.add(container); // TODO the whole solution with a list for one container is
			// ugly...
		}
	}

	@Override
	public void removeUpdateListener() {
		if (multipleAttributes) {
			throw new UnsupportedOperationException(); // TODO implement this method
		}
		else {
			deviceAttribute.setListener(false);
			recordReceivedListener = null;
			recordContainerList.clear();
		}
	}

	public void updateListener() {
		// TODO adjust quality
		if (multipleAttributes) {
			throw new UnsupportedOperationException(); // TODO implement this method
		}
		else {
			Record record = new Record(deviceAttribute.getValue(), 
					deviceAttribute.getValueTimestamp(), Flag.VALID);
			for (ChannelRecordContainer recordContainer : recordContainerList) {
				recordContainer.setRecord(record);
			}
			recordReceivedListener.newRecords(recordContainerList);
		}
	}
}
