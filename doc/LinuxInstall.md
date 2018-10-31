![emonmuc header](img/emonmuc-logo.png)

This document describes how to install the  (**e**nergy **mon**itoring **m**ulty **u**tility **c**ommunication), an open-source protocoll driver project to enable the communication with a variety of metering or other devices, developed based on the [OpenMUC](https://www.openmuc.org/) project.


---------------

# 1 Install OpenHomeMatic

This short documentation will assume the generic **version 1.0.0** of the driver as a simplification.
To install the OSGi bundle, simply download it into the emonmuc frameworks *bundles* directory

~~~shell
wget --quiet --show-progress --directory-prefix=/opt/emonmuc/bundles https://github.com/isc-konstanz/OpenHomeMatic/releases/download/v1.0.0/openmuc-driver-homematic-cc1101-1.0.0.jar
~~~

Afterwards restart the framework, for the driver to be started

~~~
emonmuc restart
~~~


#### 1.3.1 Device templates

Next, device template files are provided by this project, to ease up the configuration of some new hardware devices.  
Those can be found at *lib/device/homematic-cc1101* and should be downloaded to the corresponding directory in the emonmuc root:

~~~shell
mkdir -p /var/tmp/emonmuc/homematic-cc1101
wget --quiet --show-progress --directory-prefix=/var/tmp/emonmuc https://github.com/isc-konstanz/OpenHomeMatic/releases/download/v1.0.0/homematic-cc1101-1.0.0.zip
unzip -q /var/tmp/emonmuc/homematic-cc1101-1.0.0.zip -d /var/tmp/emonmuc/homematic-cc1101
mv -f /var/tmp/emonmuc/homematic-cc1101/lib/device/homematic-cc1101 /opt/emonmuc/lib/device/homematic-cc1101
rm -rf /var/tmp/emonmuc/homematic-cc1101
~~~
