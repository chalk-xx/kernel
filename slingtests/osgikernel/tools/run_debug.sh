#!/bin/sh
java -Xmx512m -server -Xdebug -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n -jar app/target/org.sakaiproject.kernel.app-0.1-SNAPSHOT.jar -f -

