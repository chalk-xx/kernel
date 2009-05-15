This is a binary distribution of ${K2-binary-version} based on Apache Sling.

Before you start Sakai K2, you should look at system.cfg.  
Currently, this is the only file that you should need to edit.

To start:

Linux, OSX, UNIX: run ./startup.sh
Windows: run ./startup.bat

Once the kernel startup is complete, you can access the Sling Launchpad in a web browser at:

http://localhost:8080.  

The Felix web management console is available at:

http://localhost:8080/system/console/

Log in using the default administrator's account (username: admin; password: admin).  

The Felix console provides management access to bundles, components, configuration, deployment packages, 
licensing, logging and events, OSGi repository, script engines (e.g., JavaScript, JSP, Groovy), 
threading, system info, etc.

To stop:

Linux, OSX, UNIX: run ./shutdown.sh
Windows: run ./shutdown.bat



