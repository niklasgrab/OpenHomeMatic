package org.ogema.driver.homematic.data;

public interface UpdateListener {

	public void valueChanged(TimeValue record, String address);
}
