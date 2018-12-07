package org.ogema.driver.homematic;

public class HomeMaticException extends Exception {

	private static final long serialVersionUID = 1L;

	public HomeMaticException() {
	        super();
	    }

	    public HomeMaticException(String s) {
	        super(s);
	    }

	    public HomeMaticException(Throwable cause) {
	        super(cause);
	    }

	    public HomeMaticException(String s, Throwable cause) {
	        super(s, cause);
	    }
}
