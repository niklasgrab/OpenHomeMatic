![emonmuc header](doc/img/emonmuc-logo.png)

This project implements a communication protocol as part of [emonmuc](https://github.com/isc-konstanz/emonmuc/) (**e**nergy **mon**itoring **m**ulty **u**tility **c**ommunication), based on the open-source project [OpenMUC](https://www.openmuc.org/), a software framework based on Java and OSGi, that simplifies the development of customized *monitoring, logging and control* systems.


----------

# OpenHomeMatic

[HomeMatic by eQ-3](https://www.eq-3.de/produkte/homematic.html) is a Smart Home System, operating on 868-MHz. At this relatively low frequency, highly stable transmissions over several 100m are possible with the signal being immune to disturbances from Wifi or similar radio signals.  

This project allow to read and control HomeMatic devices inside the emonmuc framework, allowing them to be controlled by other applications or energy management systems. To enable communication over 868-MHz, the installation of a radio transceiver is necessary.  
Recommended and tested hardware are e.g. CC1101 RF transceivers:

- [Stackable CC1101 (SCC) module for Raspberry Pi](http://busware.de/tiki-index.php?page=SCC)
- [CC1101 USB Lite (CUL) module](http://busware.de/tiki-index.php?page=CUL)


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
If there is the need to manually install the driver, the separate [installation guide](doc/LinuxInstall.md) may be followed.


### 1.1 Configuration



### 1.2 First Steps

With the protocol driver being enabled, some first steps can be taken to learn about the features of this project.  
For this purpose, a [First Steps guide](doc/FirstSteps.md) was documented to be followed.


----------

# Development


----------

# Contact

This project is maintained by:

![ISC logo](doc/img/isc-logo.png)

- **[ISC Konstanz](http://isc-konstanz.de/)** (International Solar Energy Research Center)
- **Adrian Minde**: adrian.minde@isc-konstanz.de
