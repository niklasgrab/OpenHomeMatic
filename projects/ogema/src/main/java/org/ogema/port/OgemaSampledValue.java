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

import org.ogema.core.channelmanager.driverspi.ChannelLocator;
import org.ogema.core.channelmanager.driverspi.SampledValueContainer;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;

public class OgemaSampledValue {

	public static Record decode(SampledValue value) {
		Flag flag = Flag.VALID;
		if (value.getValue() == null) {
			flag = Flag.NO_VALUE_RECEIVED_YET;
		}
		else if (value.getQuality() == Quality.BAD) {
			flag = Flag.UNKNOWN_ERROR;
		}
		return new Record(OgemaValue.decode(value.getValue()), value.getTimestamp(), flag);
	}
	
	public static SampledValueContainer encodeContainer(ChannelRecordContainer container) {
		ChannelLocator channelLocator = new ChannelLocator(container.getChannelAddress(), null);
		SampledValueContainer sampledValueContainer = new SampledValueContainer(channelLocator);
		sampledValueContainer.setSampledValue(encode(container.getRecord()));
		return sampledValueContainer;
	}
	
	public static SampledValue encode(Record in) {
		Quality q = Quality.GOOD;
		if (in.getFlag() != Flag.VALID) {
			q = Quality.BAD;
		}
		return new SampledValue(OgemaValue.encode(in.getValue()), in.getTimestamp(), q);
	}
	
}
