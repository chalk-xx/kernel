#!/bin/bash
[[ -n "$1" ]] || { echo "Usage: load-data.sh <server host> <server port>"; exit 0 ; }

HOST=$1
PORT=$2

NUMUSERS=`wc -l netids01.csv | awk '{print $1}'`
echo loading $NUMUSERS users to http://$HOST:$PORT
perl ../usersfromcsv.pl netids01.csv $HOST $PORT

NUMMESSAGES=`wc -l recipients.csv | awk '{print $1}'`
echo loading $NUMMESSAGES messages
jmeter --nongui -l loadmessageslog.txt --testfile load-messages.jmx

NUMCONTENT=`wc -l content.txt | awk '{print $1}'`
echo loading $NUMCONTENT files
jmeter --nongui -l loadfileslog.txt --testfile load-files.jmx

NUMTAGS=`wc -l user-tags.csv | awk '{print $1}'`
echo loading $NUMTAGS tags
jmeter --nongui -l loadtagslog.txt --testfile load-tags.jmx


