package org.ogema.driver.homematic.connection;

import java.io.IOException;

import org.ogema.driver.homematic.manager.InputOutputFifo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SerialConnection implements Connection, ConnectionListener {
	private final Logger logger = LoggerFactory.getLogger(SerialConnection.class);

	protected static final String SERIAL_PORT = "org.ogema.driver.homematic.serial.port";

	protected volatile InputOutputFifo<byte[]> fifo;
	protected volatile Object lock;
	protected boolean closed = true;

	public SerialConnection() {
		this.fifo = new InputOutputFifo<byte[]>(6);
		this.lock = new Object();
	}

	public boolean isClosed() {
		return this.closed;
	}

	@Override
	public void open() throws IOException {
		if (isClosed()) {
			closed = false;
			try {
				openPort();
				
//				sendFrame("X71".getBytes());
				sendFrame("Ar".getBytes());
				sendFrame("V".getBytes());
		 		
			} catch (IOException e) {
				closed = true;
				close();
				throw e;
			}
		}
	}

	protected abstract void openPort() throws IOException;

	@Override
	public void close() {
		closed = true;
		try {
			closePort();
			
		} catch (IOException e) {
			logger.warn("Error closing port: {}", e.getMessage());
		}
	}

	protected abstract void closePort() throws IOException;

	@Override
	public Object getReceivedLock() {
		return lock;
	}

	@Override
	public void onReceivedFrame(byte[] frame) {
		fifo.put(frame);
		synchronized (lock) {
			lock.notify();
		}
	}

	@Override
	public boolean hasFrames() {
		return fifo.getCount() > 0 ? true : false;
	}

	@Override
	public byte[] getReceivedFrame() {
		return fifo.get();
	}

	@Override
	public void sendFrame(byte[] frame) {
		try {
			// We have always to send CR and LF at the end of the data
			byte[] dataCRLF = new byte[frame.length+2];
			for (int i = 0; i < frame.length; i++) {
				dataCRLF[i] = frame[i];
			}
			dataCRLF[dataCRLF.length-2]= 13; // CR appended
			dataCRLF[dataCRLF.length-1]= 10; // LF appended
			
			write(dataCRLF);
			
		} catch (IOException e) {
			logger.warn("Error while sending frame: {}", new String(frame));
		}
	}

	protected abstract void write(byte[] data) throws IOException;

}
