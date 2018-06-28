package org.ogema.driver.homematic.connection.serial;

import java.io.IOException;
import java.util.TooManyListenersException;

import org.ogema.driver.homematic.connection.ProtocolType;
import org.ogema.driver.homematic.usbconnection.Fifo;
import org.ogema.driver.homematic.usbconnection.IUsbConnection;
import org.slf4j.Logger;

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


public class SerialRaspiPinConnection implements IUsbConnection {

	private final Logger logger = org.slf4j.LoggerFactory.getLogger("homematic-driver");

	protected volatile Fifo<byte[]> inputFifo;
	protected volatile Object inputEventLock;

	private boolean closed = true;
    private GpioController gpio;
    private GpioPinDigitalOutput pin;
    private Serial port;

	private SerialDataEventListener lsnr;
	
	public SerialRaspiPinConnection(final ProtocolType asksin) {
		inputFifo = new Fifo<byte[]>(6);
		inputEventLock = new Object();
		lsnr = new HMSerialDataEventListener();
	}

	public boolean isClosed() {
		return this.closed;
	}

	public void open() throws IOException, TooManyListenersException {
		if (isClosed()) {
			try {
		    	logger.debug("Opening connection to Pi serial port");
		    	
		        // provision gpio pin #17 as an output pin and turn on
		    	gpio = GpioFactory.getInstance();
		        pin = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "CUL", PinState.HIGH);
		        pin.setShutdownOptions(true, PinState.LOW);
		        
		        try {
		            Thread.sleep(1000);
		        } catch (InterruptedException e) {
		        	closeConnection();
					throw new SerialPortException("Unable to open Pi port: " + e.getMessage());
				}

		        // register the serial data listener
		        port = SerialFactory.createInstance();
		        port.addListener(lsnr);
		        
				port.open(Serial.DEFAULT_COM_PORT, 38400);
				
				write("V".getBytes());
//				write("X71".getBytes());
//				BINARY_MODE = true;
				write("Ar".getBytes());

//				keepAlive = new SerialKeepAlive(this);
//				keepAliveThread = new Thread(keepAlive);
//				keepAliveThread.setName("homematic-lld-keepAlive");
//				keepAliveThread.start();
			} catch (IOException | RuntimeException e) {
				closeConnection();
				throw e;
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
        
        // turn pin #17 off
        pin.low();
        pin.unexport();
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
