package org.ogema.port;

import org.ogema.core.channelmanager.driverspi.ChannelLocator;
import org.ogema.core.channelmanager.driverspi.SampledValueContainer;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;

public class OgemaSampledValue {

	public static Record decode(SampledValue in) {
		Flag flag = Flag.VALID;
		if (in.getValue() == null) {
			flag = flag.NO_VALUE_RECEIVED_YET;
		}
		else if (in.getQuality() == Quality.BAD) {
			flag = Flag.UNKNOWN_ERROR;
		}
		return new Record(OgemaValue.decode(in.getValue()), in.getTimestamp(), flag);
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
