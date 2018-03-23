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
package org.ogema.driver.homematic.connection.serial;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

import org.ogema.driver.homematic.connection.ProtocolType;
import org.ogema.driver.homematic.usbconnection.Fifo;
import org.ogema.driver.homematic.usbconnection.IUsbConnection;
import org.slf4j.Logger;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.SerialPortEvent;
import gnu.io.SerialPortEventListener;
import gnu.io.UnsupportedCommOperationException;

/**
 * 
 * @author Godwin Burkhardt
 * 
 */
@SuppressWarnings("deprecation")
public class SerialConnection implements IUsbConnection {

	private static final String APP_NAME = "org.openmuc.framework.driver.homematic.SERIAL";
	private static final String PORT_KEY = "serialPort";
	private static final String PORT_DEFAULT = "/dev/ttyACM0";
	private static final String BAUDRATE_KEY = "baudrate";
	private static final String BAUDRATE_DEFAULT = "9600";
	private static final String DATABITS_KEY = "databits";
	private static final String DATABITS_DEFAULT = "7";
	private static final String STOPBITS_KEY = "stopbits";
	private static final String STOPBITS_DEFAULT = "1";
	private static final String PARITY_KEY = "parity";
	private static final String PARITY_DEFAULT = "2";

	public static boolean BINARY_MODE = false;
	
	
	private final Logger logger = org.slf4j.LoggerFactory.getLogger("homematic-driver");


	private SerialPort serialPort;
	private String portName;
	private int baudrate;
	private int databits;
	private int stopbits;
	private int parity;

    private int timeout = 5000;
	private boolean closed = true;

	protected volatile Fifo<byte[]> inputFifo;
	protected volatile Object inputEventLock;

    private InputStream inputStream;
    private OutputStream outputStream;
	
	private SerialPortEventListener lsnr;
	
	private SerialKeepAlive keepAlive;
	private Thread keepAliveThread;
	
	private ProtocolType protocolType = ProtocolType.OTHER;
	
	
	public SerialConnection(final ProtocolType type) {
		this.portName = System.getProperty(APP_NAME + "." + PORT_KEY, PORT_DEFAULT);
		this.baudrate = Integer.parseInt(System.getProperty(APP_NAME + "." + BAUDRATE_KEY, BAUDRATE_DEFAULT));
		this.databits = Integer.parseInt(System.getProperty(APP_NAME + "." + DATABITS_KEY, DATABITS_DEFAULT));
		this.stopbits = Integer.parseInt(System.getProperty(APP_NAME + "." + STOPBITS_KEY, STOPBITS_DEFAULT));
		this.parity = Integer.parseInt(System.getProperty(APP_NAME + "." + PARITY_KEY, PARITY_DEFAULT));
		protocolType = type;
		inputFifo = new Fifo<byte[]>(6);
		inputEventLock = new Object();
		lsnr = new HMSerialPortEventListener();
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}
	
	public int getTimeout() {
		return timeout;
	}
	
	public boolean isClosed() {
		return this.closed;
	}

	public void open() throws IOException, TooManyListenersException {
		if (isClosed()) {
			try {
				serialPort = acquireSerialPort(portName);
	            inputStream = serialPort.getInputStream();
	            outputStream = serialPort.getOutputStream();
				serialPort.addEventListener(lsnr);
				serialPort.notifyOnDataAvailable(true);
				write("V".getBytes());
//				write("X71".getBytes());
//				BINARY_MODE = true;
				write("Ar".getBytes());
				
//				keepAlive = new SerialKeepAlive(this);
//				keepAliveThread = new Thread(keepAlive);
//				keepAliveThread.setName("homematic-lld-keepAlive");
//				keepAliveThread.start();
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
				// We have always to send CR and LF at the end of the data  //TODO Fix it in Interface ?
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
			commPort = portIdentifier.open(APP_NAME, 2000);
			
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
			serialPort.setSerialPortParams(baudrate, databits, stopbits, parity);
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

	public void setParameters(int baudrate) throws IOException {
		setParameters(baudrate, databits, stopbits, parity);
	}

	public void resetParameters() throws IOException {
		setParameters(baudrate, databits, stopbits, parity);
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
		if (keepAlive != null) {
			keepAlive.stop();
			keepAliveThread.interrupt();
		}
		
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
	
	class HMSerialPortEventListener implements gnu.io.SerialPortEventListener {

		@Override
		public void serialEvent(SerialPortEvent event) {
            try {
                switch (event.getEventType()) {
                    case gnu.io.SerialPortEvent.DATA_AVAILABLE:
                    	logger.debug("Notified about received data");
                    	ByteArrayOutputStream puffer = new ByteArrayOutputStream(1);
        				
                    	while(inputStream.available() > 0) {
                    		puffer.write((byte) inputStream.read());
                    	}

                    	inputFifo.put(puffer.toByteArray());
        				synchronized (inputEventLock) {
        					inputEventLock.notify();
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
                }
            }
            catch (IOException ex) {
            	logger.warn("Error while receiving data: {}", ex.getMessage());
            }
		}
	}

}
