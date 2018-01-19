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
package org.ogema.driver.homematic;

import org.ogema.core.OgemaDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HomeMaticConnection {
	private final static Logger logger = LoggerFactory.getLogger(HomeMaticConnection.class);

	public HomeMaticConnection(String address) {
		logger.info("Registered homematic test: {}", address);

		OgemaDependency dependency = new OgemaDependency();
	}

	public Object read(String foo) {
		
		return null;
	}

}
