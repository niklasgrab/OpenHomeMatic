![emonmuc header](docs/img/emonmuc-logo.png)

This project implements a communication protocol as part of [emonmuc](https://github.com/isc-konstanz/emonmuc/) (**e**nergy **mon**itoring **m**ulti **u**tility **c**ommunication), based on the open-source project [OpenMUC](https://www.openmuc.org/), a software framework based on Java and OSGi, that simplifies the development of customized *monitoring, logging and control* systems.


----------

# OpenHomeMatic

[HomeMatic by eQ-3](https://www.eq-3.de/produkte/homematic.html) is a Smart Home System, operating on 868-MHz. At this relatively low frequency, highly stable transmissions over several 100m are possible with the signal being immune to disturbances from Wifi or similar radio signals.  

This project is based on the [OGEMA](http://www.ogema.org/) project and allows to read and control HomeMatic devices inside the emonmuc framework, allowing them to be controlled by other applications or energy management systems. To enable communication over 868-MHz, the installation of a radio transceiver is necessary.  
Recommended and tested hardware are e.g. CC1101 RF transceivers:

- [**C**C1101 **U**SB **L**ite (CUL) module](http://busware.de/tiki-index.php?page=CUL)
- [**S**tackable **CC**1101 (SCC) module for Raspberry Pi](http://busware.de/tiki-index.php?page=SCC)

To flash and prepare the **SCC** module, a comprehensive [firmware installation guide](docs/FirmwareSCC.md) may be followed.


## 1 Installation

To setup this protocol driver, [emonmuc](https://github.com/isc-konstanz/emonmuc/) needs to be installed. To do so, a comprehensive guide is provided on the projects GitHub page.

With emonmuc being installed, the driver may be enabled

~~~
emonmuc install homematic-cc1101
~~~

To disable the driver, use

~~~
emonmuc remove homematic-cc1101
~~~

This shell command will set up the driver, as instructed in the [setup script](setup.sh).  
If there is the need to manually install the driver, a separate [installation guide](docs/LinuxInstall.md) may be followed.


### 1.1 Serial Port

To use any serial port with the emonmuc framework, the open-source project [jRxTx](https://github.com/openmuc/jrxtx) is used. This, as well as some additional steps to use the UART Pins of the Raspberry Pi Platform, need to be prepared.  
The [Serial Port preparation guide](https://github.com/isc-konstanz/emonmuc/blob/master/docs/LinuxSerialPort.md) needs to be followed to do so.


### 1.2 Wiring Pi

For the **S**tackable **CC**1101 (SCC) module for the Raspberry Pi, the GPIOs 0 and 1 needs to be set. This is done with the [Pi4J](https://www.pi4j.com/) library, that links to the [Wiring Pi](http://wiringpi.com/) debian package. To prepare it, the [Wiring Pi preparation guide](https://github.com/isc-konstanz/emonmuc/blob/master/docs/LinuxWiringPi.md) needs to be followed.


### 1.3 Configuration

Depending on the RF transceiver used, some additional configurations may be necessary.  
This can be done in the OSGi frameworks system properties, located by default in `/opt/emonmuc/conf/system.properties`. Add any of the optional properties like this:

~~~ini
# Define the hardware interface used: <SCC/CUL>. Default is SCC
org.openmuc.framework.driver.homematic.interface = CUL

# If the CUL interface is used, the serial port of the stick needs to be defined. Default for CUL is /dev/ttyUSB0
;org.ogema.driver.homematic.serial.port = /dev/ttyACM0
~~~

Both hardware interface communication options **SCC** and **CUL** are serial connections.  
The property `org.ogema.driver.homematic.serial.port` defines the port to be used. For *CUL* the default value of the port is */dev/ttyUSB0*. For *SCC*, the default value of the port is */dev/ttyAMA0*.

Additionally, the transceivers ID, used to pair and identify itself with HomeMatic devices, may be chosen.  
The property `org.openmuc.framework.driver.homematic.id` allows to set this ID, which can be an arbitrary string of 6 characters. The default value is *F11034*.


### 1.2 Serial Port

To use any serial port with the emonmuc framework, the open-source project [jRxTx](https://github.com/openmuc/jrxtx) is used. This, as well as some additional steps to use the UART Pins of the Raspberry Pi Platform, need to be prepared.  
The [Serial Port preparation guide](docs/LinuxSerialPort.md) needs to be followed to do so.


## 2 Guide

With the protocol driver being enabled, some first steps can be taken to learn about the features of this project.  
For this purpose, a [First Steps guide](docs/FirstSteps.md) was documented to be followed.


## 3 Development

To provide an entrypoint for further developments, a generated [javadoc](https://isc-konstanz.github.io/OpenHomeMatic/javadoc/) can be visited. 
A description of system resources and a rough architecture overview got documented in the [Development](docs/Development.md) site and is encouraged to be read.  
For other questions or initiatives please don't hesitate to file an issue or get into contact directly.

Currently, only a set of HomeMatic devices are supported and provided with templates to be created with the [emoncms device module](https://github.com/emoncms/device). While others should work as well, the only tested devices are:

  - Switch Actuator (with power metering):
    -  HM-ES-PMSw1-Pl
  - Thermostat:
    - HM-CC-RT-DN
  - Temperature/Humidity Sensor:
    - HM-WDS30-T-O
    - HM-WDS40-TH-I-2


----------

# Contact

This project is maintained by:

![ISC logo](docs/img/isc-logo.png)

- **[ISC Konstanz](http://isc-konstanz.de/)** (International Solar Energy Research Center)
- **Adrian Minde**: adrian.minde@isc-konstanz.de
