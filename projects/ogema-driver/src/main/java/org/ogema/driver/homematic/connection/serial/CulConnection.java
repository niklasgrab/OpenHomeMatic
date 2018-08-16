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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import org.ogema.driver.homematic.connection.ProtocolType;
import org.ogema.driver.homematic.usbconnection.Fifo;
import org.ogema.driver.homematic.usbconnection.IUsbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

@SuppressWarnings("deprecation")
public class CulConnection implements IUsbConnection {
	private final Logger logger = LoggerFactory.getLogger(CulConnection.class);

	public static boolean BINARY_MODE = false;

	private static final String SERIAL_NAME = "org.openmuc.framework.driver.homematic.cul";
	private static final int SERIAL_BAUDRATE = 9600;
	private static final int SERIAL_DATA_BITS = 7;
	private static final int SERIAL_STOP_BITS = 1;
	private static final int SERIAL_PARITY = 2;

	private SerialPort serialPort;
	private String serialPortName;

	private boolean closed = true;

	protected volatile Fifo<byte[]> inputFifo;
	protected volatile Object inputEventLock;

	private BufferedReader input;
	private OutputStream outputStream;

	private SerialPortEventListener lsnr;

//	private SerialKeepAlive keepAlive;
//	private Thread keepAliveThread;

	private ProtocolType protocolType = ProtocolType.BYTE;

	public CulConnection(final ProtocolType type) {
		this.protocolType = type;
		this.inputFifo = new Fifo<byte[]>(6);
		this.inputEventLock = new Object();
		this.lsnr = new CulEventListener();
	}

	public boolean isClosed() {
		return this.closed;
	}

	public void open() throws IOException, TooManyListenersException {
		if (isClosed()) {
			try {
				serialPort = acquireSerialPort(serialPortName);
				input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
				outputStream = serialPort.getOutputStream();
				serialPort.addEventListener(lsnr);
				serialPort.notifyOnDataAvailable(true);
				
//				write("X71".getBytes());
//				BINARY_MODE = true;
				write("Ar".getBytes());
				write("V".getBytes());
				
			} catch (IOException |TooManyListenersException | RuntimeException e) {
				if (serialPort != null) { 
					serialPort.close();
				}
				throw e;
			}
		}
		closed = false;
	}

	public void write(final byte[] data) throws IOException {
			try {
				// We have always to send CR and LF at the end of the data  //TODO Fix it in interface ?
				byte[] dataCRLF = new byte[data.length+2];
				for (int i = 0; i < data.length; i++) {
					dataCRLF[i] = data[i];
				}
				dataCRLF[dataCRLF.length-2]= 13; // CR appended
				dataCRLF[dataCRLF.length-1]= 10; // LF appended
				
				logger.debug("write message: " + new String(dataCRLF, "UTF-8"));

				outputStream.write(dataCRLF);
				outputStream.flush();
			} catch (IOException e) {
				throw e;
			}
	}

	private SerialPort acquireSerialPort(String serialPortName) throws IOException {
		CommPortIdentifier portIdentifier;
		try {
			portIdentifier = CommPortIdentifier.getPortIdentifier(serialPortName);
			
		} catch (NoSuchPortException e) {
			throw new IOException("The specified port does not exist", e);
		}

		CommPort commPort;
		try {
			if (portIdentifier.isCurrentlyOwned()) {
				System.out.println("Port is currently owned by: " + portIdentifier.getCurrentOwner());
			}
			commPort = portIdentifier.open(SERIAL_NAME, 2000);
			
		} catch (PortInUseException e) {
			throw new IOException("The specified port is already in use", e);
		}
		
		if (!(commPort instanceof SerialPort)) {
			// may never be the case
			commPort.close();
			throw new IOException("The specified CommPort is not a serial port");
		}
		
		try {
			SerialPort serialPort = (SerialPort) commPort;
			
			serialPort.setSerialPortParams(SERIAL_BAUDRATE, SERIAL_DATA_BITS, SERIAL_STOP_BITS, SERIAL_PARITY);
			serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
			serialPort.disableReceiveTimeout();
			serialPort.enableReceiveThreshold(1);
			
			return serialPort;
			
		} catch (UnsupportedCommOperationException e) {
			if (commPort != null) {
				commPort.close();
			}
			throw new IOException("Unable to set the baud rate or other serial port parameters", e);
		}
	}

	public void setParameters(int baudrate, int dataBits, int stopBits, int parity) throws IOException {
		if (serialPort.getBaudRate() != baudrate ||
				serialPort.getDataBits() != dataBits || serialPort.getStopBits() != stopBits || serialPort.getParity() != parity) {

			try {
				serialPort.setSerialPortParams(baudrate, dataBits, stopBits, parity);
				
			} catch (UnsupportedCommOperationException e) {
				throw new IOException("Unable to set the baud rate or other serial port parameters", e);
			}
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
//		if (keepAlive != null) {
//			keepAlive.stop();
//			keepAliveThread.interrupt();
//		}
		
		if (serialPort != null) {
			serialPort.close();
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
	
	class CulEventListener implements gnu.io.SerialPortEventListener {

		@Override
		public void serialEvent(SerialPortEvent event) {
			try {
				switch (event.getEventType()) {
					case gnu.io.SerialPortEvent.DATA_AVAILABLE:
						logger.debug("Notified about received data");
						String line = null;
						if (input.ready()) {
							
							line = input.readLine();
							if (!line.isEmpty()) {
								inputFifo.put(line.getBytes());
								synchronized (inputEventLock) {
									inputEventLock.notify();
								}
							}
						}
						break;
						
					case gnu.io.SerialPortEvent.BI:
					case gnu.io.SerialPortEvent.CD:
					case gnu.io.SerialPortEvent.CTS:
					case gnu.io.SerialPortEvent.DSR:
					case gnu.io.SerialPortEvent.FE:
					case gnu.io.SerialPortEvent.OUTPUT_BUFFER_EMPTY:
					case gnu.io.SerialPortEvent.PE:
					case gnu.io.SerialPortEvent.RI:
					default:
						break;
				}
			}
			catch (IOException ex) {
				logger.warn("Error while receiving data: {}", ex.getMessage());
			}
		}
	}

}
