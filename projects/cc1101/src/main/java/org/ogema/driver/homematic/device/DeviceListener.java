package org.ogema.driver.homematic.device;

import java.util.Collection;

public interface DeviceListener {

	public void onAttributesChanged(Collection<DeviceAttribute> attributes);

}
