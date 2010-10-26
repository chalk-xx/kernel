#!/bin/bash
[[ -n "$1" ]] || { echo "Usage: load-data.sh <server host> <server port>"; exit 0 ; }

HOST=$1
PORT=$2

NUMUSERS=`wc -l netids01.csv | awk '{print $1}'`
echo loading $NUMUSERS users to http://$HOST:$PORT
perl ../usersfromcsv.pl netids01.csv $HOST $PORT

NUMMESSAGES=`wc -l message-pairs.csv | awk '{print $1}'`
echo loading $NUMMESSAGES messages
NUMMESSAGELOOPS=$(( $NUMMESSAGES / 10 ))
jmeter --nongui --testfile load-messages.jmx -JLOOPS=$NUMMESSAGELOOPS

NUMCONTENT=`wc -l content.txt | awk '{print $1}'`
echo loading $NUMCONTENT files
NUMCONTENTLOOPS=$(( $NUMCONTENT / 2 ))
jmeter --nongui --testfile load-files.jmx -JLOOPS=$NUMCONTENTLOOPS

echo loading tags


