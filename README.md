AisVirtualNet
=============

A virtual AIS network with a virtual sphere of AIS messages and virtual transponders

Prerequisites
-------------

	* Java 7
	* Maven 3

Building
--------
	
	mvn install

Developing
----------

Eclipse m2 plugin or maven eclipse target

	mvn eclipse:eclipse

Deployment
----------

Distributable packages are created in `target/` of transponder and server module.

Running
-------

Server
	
	./server.sh -conf server.xml

Server as service 

	./server-service.sh start <conffile>

Transponder console

	./transponder.sh -conf <conffile>

Transponder GUI

	./transponder-gui.sh

or

	./transponder-gui.bat


Embedding transponder
----------------------

See EPD project for example of embedding the transponder.

