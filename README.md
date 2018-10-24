![emonmuc header](doc/img/emonmuc-logo.png)

This project implements a communication protocol as part of emonmuc (**e**nergy **mon**itoring **m**ulty **u**tility **c**ommunication), an *unofficial fork* of the open-source project [OpenMUC](https://www.openmuc.org/), a software framework based on Java and OSGi, that simplifies the development of customized *monitoring, logging and control* systems.


----------

# OpenHomeMatic

*This section is a placeholder and will be filled with a project description.*


## 1 Installation

The installation guide was documented for Linux based platforms, but further guides may follow.

To setup this protocol driver, **[emonmuc](https://github.com/isc-konstanz/emonmuc/)** needs to be installed. To do so, a comprehensive guide is provided on the projects GitHub page.

With emonmuc being installed, simply copy the driver jarfile from the projects *build/lib*, to the *bundles-available* directory, by default located at `/opt/emonmuc/bundles-available`. For example:

~~~
cp ~/OpenHomeMatic/build/libs/openmuc-driver-homematic-cc1101* /opt/emonmuc/bundles-available/
~~~

Now, the driver can be enabled

~~~
emonmuc enable driver homematic-cc1101
~~~

To disable the driver, use

~~~
emonmuc disable driver homematic-cc1101
~~~


### 1.1 Device templates

Next, device template files are provided by this project, to ease up the configuration of some new hardware devices.  
Those can be found at *lib/device/homematic-cc1101* and should be copied to the corresponding directory in the emonmuc root:

~~~
cp -R ~/OpenHomeMatic/lib/device/homematic-cc1101 /opt/emonmuc/lib/device/homematic-cc1101
~~~

### 2. Software Description

### 2.1 Introduction

The project OpenHomeMatic implements the homematic driver for [OpenMUC](https://www.openmuc.org/). It is based on the [OGEMA project](http://www.ogema.org/). The most benefit we get from the OMEGA project, is that OGEMA contains a configuration and the messages for the devices, which are exchanged between the devices and the driver. Nevertheless we decide to refactor the OGEMA sources for our purpose, because we use the OpenMUC framework with the openmuc-core-api.jar and the openmuc-core-spi.jar, which are quite different to the OGEMA api part. 
In case we use refactored OGEMA sources you will find them in the same packages as in the OGEMA project. In general the structure is the same. 
The picture below shows the structure of the projects. The red polygon belongs to OpenMUC project. To the green colored open-driver-homematic-cc1101 project belongs the packages homematic and homematic.settings. The yellow colored project homematic-cc1101 contains the package homematic, data, tools, config, manager with the sub packages devices and messages and connection. The blue lines shows the dependencies between the packages. 

![package dependencies](doc/img/package-dependence.png)

### 2.2 Package homematic in project open-driver-homematic-cc1101

The package homematic is base of the driver. It instantiate the homematic manger which starts the Message Handler to connect the driver over jrxtx to the devices. It connects the driver with the OpenMUC framework. The OpenMUC framework calls the connect method of the homematic driver class to connect devices by its configuration. Therefore the connect method will get or create the device from the address and create a homematic connection. This connection is a connection to the OpenMUC framework and has nothing to do with jrxtx. The OpenMUC framework can call the method scanForDevices of the homeatic driver class to enable the receive of pairing request of the devices. In this way it is possible to pair devices with the OpenMUC framework. Only for paired devices one can get values from the channels of the device. 

The OpenMUC framework use the HomeMaticConnection class to scan for channels. Each channel contains one attribute or command of the device. The method read allows OpenMUC framework to get the record of the channel (see below 2.4). The method start listening allows the OpenMUC framework to listen to attribute value changes of the channel. With the method write the OpenMUC framework has the possibility to write a command value to the channel. For example to switch on or off of a switch plug device.  

### 2.3 Package settings in project open-driver-homematic-cc1101

### 2.4 Package homematic in project homematic-cc1101

The package contains the abstract HomeMaticChannel class. Channel classes are divided in the types attribute channel class and command channel class. Each channel has a unique address. The abstract Home_MaticChannel class creates the channel class. The channel class reads the record and return it. The record contains the of the attribute value of the channel, the time of the last update and a validation flag. It allows to set and remove an update event listener. With the write method a command value can be written to the channel.


### 2.5 Package config in project homematic-cc1101

### 2.6 Package connection in project homematic-cc1101

![class diagram connection](doc/img/connection.png)

### 2.7 Package data in project homematic-cc1101

Because the OpenMUC framework don’t support an object value class to hold an arbitrary object, therefore the package contains the ObjectValue class. This class should be moved to the package data of the OpenMUC framework. 

### 2.8 Package manager in project homematic-cc1101

![class diagram manager](doc/img/manager.png)

The package manager is the heart of the driver. When the HomeMaticManager is instantiated it creates the message handler which is responsible to receive and send messages. The HomeMaticManager holds a map of devices. 

A device represented by the abstract class Device is either a CO2Detector, a MotionDetector, a PowerMeter, a Remote Device (Concentrator), a SmokeSensor, a SwitchPlug, a Thermostat, a ThreeStateSensor or a THSensor. The classes for the concrete devices are in the sub-package devices. Each device contains an address and a map of its attributes and commands. This map is filled with the method configureChannels. Further each device knows his paring state. That means that each device knows whether it is unpaired, paring is in progress or if it is paired. The device can parse the incoming message with the method parseMessage and further parse the attribute values of contained in this message with the method parseValue. It can create a command message with the method channelChanged and call the message handler to send this message. 

The DeviceAttribute class and the DeviceCommand is extended from abstract class DeviceChannel. The DeviceAttribute class has a unique identifier. The DeviceAttribute class holds the attribute value and the time of the last update. If the status message is received the device can call setValue of the DeviceAttribute to store the value and create a new timestamp. If a listener is registered it further calls the method updateListerner of the AttributeChannel to inform the the listener over value changes. The DeviceCommand class is only for distinguishing the device channels from DeviceAttribute classes in the HomeMaticConnection class when we are scan for channels. The class ValueType contains the supported value types for the DeviceAttribute.

The MessageHandler creates and starts the InputThread for receiving messages. It creates and opens the configured Connection from the package connection. The MessageHandler can be called with one of the methods sendMessage. In this case for each device an output thread is created and started if this thread is not available. The send message is added to the outputThread. The OutputThread is responsible to send the message over the Connection Class. The The input- and the output-threads lives until the MessageHandler is stopped. When the inputThread not receives a pairing message it calls the method onReceivedMessage of the MessageHandler. The method onReceivedMessage checks if the incoming message belongs to known device and returns immediate if not so. Next we look if we have to acknowledge the outputThread by calling interrupt because this thread is waiting for a received message. If a broadcast message was sent, we sent an acknowledge message back. We look for the last sent command message and call parseMessage of the device.

In the run method of the InputThread the thread waits in a loop for incoming messages. If an incoming message was received it calls handleMessage. The first byte of the message have to start with 'V' for Version message and 'A' or 'a' for status messages. If the type of the status message is 0x00 and manager is in pairing mode we try to create a new device. In the other case if the message destination equals to the id or it is a broadcast message or the message is in party mode the message is forwarded to onReceivedMessage.
       

![activity diagram messages](doc/img/messages.png)

### 2.9 Package devices in project homematic-cc1101

This package contains the concrete devices which are derived from the abstract class Device of the package manager.

### 2.10 Package messages in project homematic-cc1101

### 2.11 Package tools in project homematic-cc1101
----------

# Contact

This project is maintained by:

![ISC logo](doc/img/isc-logo.png)

- **[ISC Konstanz](http://isc-konstanz.de/)** (International Solar Energy Research Center)
- **Adrian Minde**: adrian.minde@isc-konstanz.de
