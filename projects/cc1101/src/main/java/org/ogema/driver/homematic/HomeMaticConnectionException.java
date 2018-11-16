package org.ogema.driver.homematic;

public class HomeMaticConnectionException extends Exception {

	private static final long serialVersionUID = 1L;

	public HomeMaticConnectionException() {
	        super();
	    }

	    public HomeMaticConnectionException(String s) {
	        super(s);
	    }

	    public HomeMaticConnectionException(Throwable cause) {
	        super(cause);
	    }

	    public HomeMaticConnectionException(String s, Throwable cause) {
	        super(s, cause);
	    }
}
