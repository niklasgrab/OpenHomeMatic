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


----------

# Contact

This project is maintained by:

![ISC logo](doc/img/isc-logo.png)

- **[ISC Konstanz](http://isc-konstanz.de/)** (International Solar Energy Research Center)
- **Adrian Minde**: adrian.minde@isc-konstanz.de
