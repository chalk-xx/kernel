#!/bin/bash

## collect the tools directory reference
TOOLSDIR=`dirname $0`

## source in the version we're working on
. ${TOOLSDIR}/version

## push to the path stack the dir above tools
pushd ${TOOLSDIR}/..

if [[ "a$1" == "aall" ]]
then
   ## if "all" is on the CLI, build everythign
   echo "Building with $BUILD_OPT"
   mvn $BUILD_OPT clean install
else 
   ## otherwise, just rebundle
   tools/rebundle.sh $*
fi

if [[ $? -ne 0 ]] 
then
   ## if the last command failed, pop the path stack and exit
   popd
   exit 10
fi

## pop off the path stack the dir above 'tools'
popd

## remove the sling dir
rm -rf sling

## check for 32 bit capability
has_32_bit=`java -help | grep -c "\-d32"`
if [[ $has_32_bit == "1" ]]
then
  d32="-d32"
else
  d32=""
fi

## start the server
java  $d32 -Xmx512m -server -Dcom.sun.management.jmxremote -jar ${TOOLSDIR}/../app/target/org.sakaiproject.nakamura.app-${K2VERSION}.jar -f -

