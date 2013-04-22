#!/bin/bash

SCRIPTPATH=`dirname $0`
cd $SCRIPTPATH

if [ -z $2 ]
then
	CONFFILE=server.xml
else
	CONFFILE=$2
fi

PROCNAME="dk.dma.ais.virtualnet.server.ServerDaemon -file $CONFFILE"

stop () {
	# Find pid
	PID=`./getpid.pl "$PROCNAME"`
	if [ -z $PID ]; then
		echo "ServerDaemon not running"
		exit 1
	fi
	echo "Stopping ServerDaemon"
	kill $PID
    exit 0
}

case "$1" in
start)
	PID=`./getpid.pl "$PROCNAME"`
	if [ ! -z $PID ]; then
		echo "ServerDaemon already running"
		exit 1
	fi
    echo "Starting ServerDaemon"
    ./server.sh -file $CONFFILE > /dev/null 2>&1 &
    ;;
stop)
    stop
    ;;
restart)
    $0 stop
    sleep 1
    $0 start
    ;;
*)
    echo "Usage: $0 (start|stop|restart|help) [conffile]"
esac
