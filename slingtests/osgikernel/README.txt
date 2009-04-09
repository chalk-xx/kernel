This is the Sling based K2 that uses OSGi.

To run
mvn clean install
java  -Dorg.osgi.framework.bootdelegation="org.xml.sax,javax.xml.parsers,javax.sql,org.w3c.dom,org.xml.sax.ext,javax.naming.*,javax.xml.*" -jar app/target/org.sakaiproject.kernel2.osgi.app-0.1-SNAPSHOT.jar

This will start Felix configured with Sling and the Sakai K2 bundles.

You will find the bundles under budles and some libraries under libraries.


