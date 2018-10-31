![emonmuc header](doc/img/emonmuc-logo.png)

This project implements a communication protocol as part of [emonmuc](https://github.com/isc-konstanz/emonmuc/) (**e**nergy **mon**itoring **m**ulty **u**tility **c**ommunication), based on the open-source project [OpenMUC](https://www.openmuc.org/), a software framework based on Java and OSGi, that simplifies the development of customized *monitoring, logging and control* systems.


----------

# OpenHomeMatic

[HomeMatic by eQ-3](https://www.eq-3.de/produkte/homematic.html) is a Smart Home System, operating on 868-MHz. At this relatively low frequency, highly stable transmissions over several 100m are possible with the signal being immune to disturbances from Wifi or similar radio signals.  

This project is based on the [OGEMA](http://www.ogema.org/) project and allows to read and control HomeMatic devices inside the emonmuc framework, allowing them to be controlled by other applications or energy management systems. To enable communication over 868-MHz, the installation of a radio transceiver is necessary.  
Recommended and tested hardware are e.g. CC1101 RF transceivers:

- [Stackable CC1101 (SCC) module for Raspberry Pi](http://busware.de/tiki-index.php?page=SCC)
- [CC1101 USB Lite (CUL) module](http://busware.de/tiki-index.php?page=CUL)


## 1 Installation

To setup this protocol driver, [emonmuc](https://github.com/isc-konstanz/emonmuc/) needs to be installed. To do so, a comprehensive guide is provided on the projects GitHub page.

With emonmuc being installed, the driver may be enabled

~~~
emonmuc enable homematic-cc1101
~~~

To disable the driver, use

~~~
emonmuc disable homematic-cc1101
~~~

This shell command will set up the driver, as instructed in the [setup script](setup.sh).  
If there is the need to manually install the driver, the separate [installation guide](doc/LinuxInstall.md) may be followed.


### 1.1 Configuration

Depending on the RF transceiver used, some additional configurations may be necessary.  
This can be done in the OSGi frameworks system properties, located by default in `/opt/emonmuc/conf/system.properties`. Add any of the optional properties like this:

~~~ini
# Define the hardware interface used: <SCC/CUL>. Default is SCC
org.openmuc.framework.driver.homematic.interface = CUL

# If the CUL interface is used, the serial port of the stick needs to be defined. Default for CUL is /dev/ttyUSB0
;org.openmuc.framework.driver.homematic.connection.port = /dev/ttyACM0
~~~

Both hardware interface communication options **SCC** and **CUL** are serial connections.  
The property `org.openmuc.framework.driver.homematic.connection.port` defines the port to be used. For *CUL* the default value of the port is */dev/ttyUSB0*. For *SCC*, the default value of the port is */dev/ttyAMA0*.

Additionally, the transceivers ID, used to pair and identify itself with HomeMatic devices, may be chosen.  
The property `org.openmuc.framework.driver.homematic.id` allows to set this ID, which can be an arbitrary string of 6 characters. The default value is *F11034*.


## 2 Guide

With the protocol driver being enabled, some first steps can be taken to learn about the features of this project.  
For this purpose, a [First Steps guide](doc/FirstSteps.md) was documented to be followed.


----------

## 3 Development

To provide an entrypoint for future developments, a description of system resources and a rough architecture overview got documented in the [Development](doc/Development.md) site and is encouraged to be read.


----------

# Contact

This project is maintained by:

![ISC logo](doc/img/isc-logo.png)

- **[ISC Konstanz](http://isc-konstanz.de/)** (International Solar Energy Research Center)
- **Adrian Minde**: adrian.minde@isc-konstanz.de
