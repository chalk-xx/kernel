#!/bin/bash
TOOLSDIR=`dirname $0`
pushd ${TOOLSDIR}/..
if [[ "a$1" == "aall" ]]
then
   mvn clean install
else 
   tools/rebundle.sh $*
fi 
if [[ $? -ne 0 ]] 
then
   popd
   exit 10
fi
popd
rm -rf sling
has_32_bit=`java -help | grep -c "\-d32"`
if [[ $has_32_bit == "1" ]]
then
  d32="-d32"
else
  d32=""
fi
java  $d32 -Xmx512m -server -Dcom.sun.management.jmxremote -jar ${TOOLSDIR}/../app/target/org.sakaiproject.kernel.app-0.1-SNAPSHOT.jar -f -

