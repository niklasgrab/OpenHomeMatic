package org.ogema.driver.homematic.connection;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.ogema.driver.homematic.manager.InputOutputFifo;
import org.openmuc.jrxtx.SerialPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SerialConnection implements Connection, ConnectionListener {
	private final Logger logger = LoggerFactory.getLogger(SerialConnection.class);

	protected volatile InputOutputFifo<byte[]> fifo;
	protected Object lock;

	protected SerialInputThread listener;
	protected SerialPort serial;
	protected OutputStream output;

	public SerialConnection() {
		this.fifo = new InputOutputFifo<byte[]>(6);
		this.lock = new Object();
	}

	@Override
	public void open() throws IOException {
		try {
			serial = build();
			output = serial.getOutputStream();
		} catch (IOException e) {
			close();
			throw e;
		}
		listener = new SerialInputThread(this, serial.getInputStream());
		listener.start();
	}

	protected abstract SerialPort build() throws IOException;

	@Override
	public void close() {
		try {
			if (listener != null) listener.close();
			output.close();
			serial.close();
			
		} catch (IOException | NullPointerException e) {
			logger.warn("Error closing port: {}", e.getMessage());
		}
	}

	@Override
	public void onDisconnect() {
		try {
			close();
			open();
			
		} catch (IOException e) {
			logger.error("Fatal error while reopening serial port");
		}
	}

	@Override
	public void onReceivedFrame(byte[] frame) {
		fifo.put(frame);
		synchronized (lock) {
			lock.notify();
		}
	}

	@Override
	public Object getReceivedLock() {
		return lock;
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
			byte[] data = Arrays.copyOf(frame, frame.length+2);
			data[data.length-2]= (byte) 0x0D; // CR appended
			data[data.length-1]= (byte) 0x0A; // LF appended
			
			write(data);
			
		} catch (IOException e) {
			logger.warn("Error while sending frame: {}", new String(frame));
		}
	}

	protected void write(byte[] data) throws IOException {
		output.write(data);
		output.flush();
	}

}
