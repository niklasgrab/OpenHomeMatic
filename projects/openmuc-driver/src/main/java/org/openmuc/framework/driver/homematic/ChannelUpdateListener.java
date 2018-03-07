package org.openmuc.framework.driver.homematic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.driverspi.SampledValueContainer;
import org.ogema.port.OgemaSampledValue;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;

public class ChannelUpdateListener implements org.ogema.core.channelmanager.driverspi.ChannelUpdateListener {

	private RecordsReceivedListener listener;
	private Map<String, ChannelRecordContainer> containerMap = new HashMap<String, ChannelRecordContainer>();
	private List<ChannelRecordContainer> containers;
	
	public ChannelUpdateListener(List<ChannelRecordContainer> containers, RecordsReceivedListener listener) {
		this.listener = listener;
		this.containers = containers;
		for (ChannelRecordContainer channelRecordContainer : containers) {
			containerMap.put(channelRecordContainer.getChannelAddress(), channelRecordContainer);
		}
	}

	@Override
	public void channelsUpdated(List<SampledValueContainer> channels) {
		for (SampledValueContainer sampledValueContainer : channels) {
			String channelAddress = sampledValueContainer.getChannelLocator().getChannelAddress();
			ChannelRecordContainer container = containerMap.get(channelAddress);
			container.setRecord(OgemaSampledValue.decode(sampledValueContainer.getSampledValue()));
		}
		
		listener.newRecords(containers);
	}

	@Override
	public void exceptionOccured(Exception e) {
		//TODO ??? listener.connectionInterrupted(arg0, arg1);

	}
}
