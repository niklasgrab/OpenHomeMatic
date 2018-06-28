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
package org.ogema.driver.homematic.connection.serial;

import java.io.IOException;
import java.util.TooManyListenersException;

import org.ogema.driver.homematic.connection.ProtocolType;
import org.ogema.driver.homematic.usbconnection.Fifo;
import org.ogema.driver.homematic.usbconnection.IUsbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.serial.Serial;
import com.pi4j.io.serial.SerialDataEvent;
import com.pi4j.io.serial.SerialDataEventListener;
import com.pi4j.io.serial.SerialFactory;
import com.pi4j.io.serial.SerialPortException;


public class SccConnection implements IUsbConnection {
	private final Logger logger = LoggerFactory.getLogger(SccConnection.class);

	protected volatile Fifo<byte[]> inputFifo;
	protected volatile Object inputEventLock;

	private boolean closed = true;
	private GpioController gpio;
	private GpioPinDigitalOutput pin;
	private Serial port;

	private SerialDataEventListener lsnr;

	private ProtocolType protocolType = ProtocolType.BYTE;

	public SccConnection(final ProtocolType type) {
		this.protocolType = type;
		this.inputFifo = new Fifo<byte[]>(6);
		this.inputEventLock = new Object();
		this.lsnr = new HMSerialDataEventListener();
	}

	public boolean isClosed() {
		return this.closed;
	}

	public void open() throws IOException, TooManyListenersException {
		if (isClosed()) {
			try {
				logger.debug("Opening connection to Pi serial port");
				
				// Provision gpio pin #17 as an output pin and turn on
				gpio = GpioFactory.getInstance();
				pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "SSC", PinState.HIGH);
				pin.setShutdownOptions(true, PinState.LOW);
				
				try {
					Thread.sleep(1000);
					
				} catch (InterruptedException e) {
					closeConnection();
					throw new IOException("Unable to open Pi port: " + e.getMessage());
				}
				
				// Register the serial data listener
				port = SerialFactory.createInstance();
				port.addListener(lsnr);
				
				port.open(Serial.DEFAULT_COM_PORT, 38400);
				
//				write("V".getBytes());
//				write("X71".getBytes());
//				BINARY_MODE = true;
				write("Ar".getBytes());
				
			} catch (IOException | RuntimeException e) {
				logger.warn("Error while opening connection: {}", e.getMessage());
				closeConnection();
			}
		}
		closed = false;		
	}

	public void write(final byte[] data) throws IOException {
		if (port == null) throw new SerialPortException("Serial port is not open");
		
		try {
			// We have always to send CR and LF at the end of the data  //TODO Fix it in Interface ?
			byte[] dataCRLF = new byte[data.length+2];
			for (int i = 0; i < data.length; i++) {
				dataCRLF[i] = data[i];
			}
			dataCRLF[dataCRLF.length-2]= 13; // CR appended
			dataCRLF[dataCRLF.length-1]= 10; // LF appended
			String dataStr = new String(dataCRLF, "UTF-8");
			
			logger.debug("write message: " + dataStr);

			port.writeln(dataStr);
			port.flush();
		} catch (IOException e) {
			throw e;
		}
	}

	public ProtocolType getProtocolType() {
		return protocolType;
	}

	@Override
	public byte[] getReceivedFrame() {
		return inputFifo.get();
	}

	@Override
	public void sendFrame(byte[] frame) {
		try {
			write(frame);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public void closeConnection() {
		try {
			if (port != null) port.close();
			
		} catch (IllegalStateException | IOException e) {
			logger.warn("Error while closing Pi port: {}", e.getMessage());
		}
		
		// Unprovision pin #17
		if (pin != null) {
			pin.low();
			pin.unexport();
		}
		closed = true;
	}

	@Override
	public Object getInputEventLock() {
		return inputEventLock;
	}

	@Override
	public boolean hasFrames() {
		return inputFifo.getCount() > 0 ? true : false;
	}

	@Override
	public void setConnectionAddress(String address) {
//		this.keepAlive.setConnectionAddress(address);
	}

	class HMSerialDataEventListener implements SerialDataEventListener {

		@Override
		public void dataReceived(SerialDataEvent event) {
			if (logger.isTraceEnabled()) {
				logger.trace("Notified about received data");
			}
			try {
				String data = event.getAsciiString();
				if (!data.isEmpty()) {
					inputFifo.put(data.trim().getBytes());
					synchronized (inputEventLock) {
						inputEventLock.notify();
					}
				}
			} catch (IOException e) {
				logger.warn("Failed reading serial event data");
			}
		}
	}

}
