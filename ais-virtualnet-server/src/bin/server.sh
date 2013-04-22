#!/bin/sh

if [ -z $JAVA_OPTS ]
then
	JAVA_OPTS="-Xmn128M -Xms512M -Xmx512M"
fi

if [ -z $LOG_CONF ]
then
	LOG_CONF="file:log4j.xml"
fi

java -Dlog4j.configuration=$LOG_CONF -cp ".:lib/*:./*" $JAVA_OPTS dk.dma.ais.virtualnet.server.ServerDaemon $@
