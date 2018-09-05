package org.ogema.driver.homematic.connection.serial;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.ogema.driver.homematic.usbconnection.Fifo;
import org.ogema.driver.homematic.usbconnection.IUsbConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SerialConnectionHandler implements IUsbConnection {
	private final Logger logger = LoggerFactory.getLogger(CulConnection.class);

	private volatile Fifo<byte[]> inputFifo;
	private volatile Object inputEventLock;

	private boolean closed = true;

    protected AutoCloseable closeable;
    private DataOutputStream os;
    private DataInputStream is;

    private class MsgReceiver extends Thread {

        @Override
        public void run() {
            while (!isClosed()) {
                int numBytesInStream;
                byte[] bytesInStream = null;
                try {
                    numBytesInStream = is.available();
                    if (numBytesInStream > 0) {
                        bytesInStream = new byte[numBytesInStream];
                        is.read(bytesInStream);
               	
//					String line = "";
//						while (true) {
//							char next = (char)is.read();
//							line += next;
//							if(next == '\r' || next == '\n') {
//							    break;
//							}
//						}
//						if (!line.isEmpty()) {
                    		String line = new String(bytesInStream);
                    		logger.info("read msg: " + line);
		                    inputFifo.put(bytesInStream);
							synchronized (inputEventLock) {
								inputEventLock.notify();
							}
//						}
                    }
                }
                catch (IOException e) {
    				closeConnection();
    				e.printStackTrace();
    				return;
               }
            }
        }

    }

   public SerialConnectionHandler() {
		this.inputFifo = new Fifo<byte[]>(6);
		this.inputEventLock = new Object();
	}

	public boolean isClosed() {
		return this.closed;
	}
	
	protected abstract AutoCloseable createSerialPort() throws IOException;
	protected abstract DataInputStream getDataInputStream() throws IOException;
	protected abstract DataOutputStream getDataOutputStream() throws IOException;

	public void open() throws Exception {
		if (isClosed()) {
			try {
		        closeable = createSerialPort();
		        

		        is = getDataInputStream();
		        os = getDataOutputStream();
				closed = false;
		        new MsgReceiver().start();
//				write("X71".getBytes());
				write("Ar".getBytes());
				write("V".getBytes());
		 				
			} catch (IOException e) {
				closeConnection();
				throw e;
			}
		}
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
			
			logger.info("write message: " + new String(dataCRLF, "UTF-8"));

			os.write(dataCRLF);
			os.flush();
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
//		if (keepAlive != null) {
//			keepAlive.stop();
//			keepAliveThread.interrupt();
//		}
		
		if (closeable != null) {
			try {
				closeable.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
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

}
