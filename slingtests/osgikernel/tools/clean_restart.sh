#!/bin/sh
tools/rebundle.sh $*
if [[ $? -ne 0 ]] 
then
   exit 10
fi
rm -rf sling
java -Xmx512m -server -jar app/target/org.sakaiproject.kernel.app-0.1-SNAPSHOT.jar -f -

