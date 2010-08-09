This is the event explorer subsystem orrginally developed by Ashish Mittal as part of the GSoC 2010 work.


It contains an AMQ client listener and a UI.


Building
--------

mvn clean install


Running
-------

java -jar explorer/app/target/org.sakaiproject.nakamura.eventexplorer.app-0.7-SNAPSHOT-sources.jar -p 8081 -f -

Logging will come out to screen and the http service will be on 8081. 
Nakamura is configured to listen on 8080, so this avoids a conflict.


