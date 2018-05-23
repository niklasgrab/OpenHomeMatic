/*
 * Copyright 2016-17 ISC Konstanz
 *
 * This file is part of OpenSkeleton.
 * For more information visit https://github.com/isc-konstanz/OpenSkeleton.
 *
 * OpenSkeleton is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenSkeleton is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenSkeleton.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.ogema.port;

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
