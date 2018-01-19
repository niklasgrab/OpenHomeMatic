/*
 * Copyright 2017-18 ISC Konstanz
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
package org.openmuc.framework.driver.homematic;

import java.util.List;

import org.ogema.driver.homematic.HomeMaticConnection;
import org.openmuc.framework.config.ArgumentSyntaxException;
import org.openmuc.framework.config.ChannelScanInfo;
import org.openmuc.framework.config.ScanException;
import org.openmuc.framework.data.DoubleValue;
import org.openmuc.framework.data.Flag;
import org.openmuc.framework.data.Record;
import org.openmuc.framework.data.Value;
import org.openmuc.framework.driver.homematic.options.HomeMaticChannelPreferences;
import org.openmuc.framework.driver.homematic.options.HomeMaticDriverInfo;
import org.openmuc.framework.driver.spi.ChannelRecordContainer;
import org.openmuc.framework.driver.spi.ChannelValueContainer;
import org.openmuc.framework.driver.spi.Connection;
import org.openmuc.framework.driver.spi.ConnectionException;
import org.openmuc.framework.driver.spi.RecordsReceivedListener;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
public class HomeMaticDevice implements Connection {
	private final static Logger logger = LoggerFactory.getLogger(HomeMaticDevice.class);
    private final HomeMaticDriverInfo info = HomeMaticDriverInfo.getInfo();

	private final HomeMaticConnection connection;

	public HomeMaticDevice(String address) {
		this.connection = new HomeMaticConnection(address);
		
	}

	@Override
	public List<ChannelScanInfo> scanForChannels(String settings)
			throws UnsupportedOperationException, ArgumentSyntaxException, ScanException, ConnectionException {
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object read(List<ChannelRecordContainer> containers, Object containerListHandle, String samplingGroup)
			throws UnsupportedOperationException, ConnectionException {

        long samplingTime = System.currentTimeMillis();

        for (ChannelRecordContainer container : containers) {
            try {
                HomeMaticChannelPreferences prefs = info.getChannelPreferences(container);
                
                String foo = prefs.getFoo();
                int bar = prefs.getBar();

                Value value = new DoubleValue((double) connection.read(foo));
                container.setRecord(new Record(value, samplingTime, Flag.VALID));

            } catch (ArgumentSyntaxException e) {
                logger.warn("Unable to configure channel address \"{}\": {}", container.getChannelAddress(), e);
                container.setRecord(new Record(null, samplingTime, Flag.DRIVER_ERROR_READ_FAILURE));
            }
        }
        
        return null;
	}

	@Override
	public void startListening(List<ChannelRecordContainer> containers, RecordsReceivedListener listener)
			throws UnsupportedOperationException, ConnectionException {
		
		// TODO Auto-generated method stub
		
	}

	@Override
	public Object write(List<ChannelValueContainer> containers, Object containerListHandle)
			throws UnsupportedOperationException, ConnectionException {
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disconnect() {
		
		// TODO Auto-generated method stub
		
	}
}
