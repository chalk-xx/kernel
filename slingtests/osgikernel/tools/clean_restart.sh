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
java -Xmx512m -server -jar ${TOOLSDIR}/../app/target/org.sakaiproject.kernel.app-0.1-SNAPSHOT.jar -f -

