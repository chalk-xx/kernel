#!/bin/sh

if [ ! -d ../jakarta-jmeter-2.4 ]
then
  echo "This run script assumes jmeter installed at the testscripts/jmeter level"
  exit 1
fi

if [ ! -d content ]
then
   curl https://files.nyu.edu/maw1/public/kern-1306/content.tar.gz > content.tar.gz
   tar xvzf content.tar.gz 
fi


if [ ! -d results ] 
then 
   mkdir results
fi

../jakarta-jmeter-2.4/bin/jmeter -n -l results/load-files.jtl -t load-files.jmx
