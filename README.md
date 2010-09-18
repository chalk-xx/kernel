This is the master branch of Nakamura maintained by Ian Boston. 
It contains some differences from the central Sakai Master branch of Nakamura where
some of those central patches have not been merged or have been reverted.

This is the Sling-based Sakai 3 kernel that uses OSGi.


To run:

    mvn clean install
    . tools/version
    java -jar app/target/org.sakaiproject.nakamura.app-${K2VERSION}.jar

This will start Felix configured with Sling and the Sakai nakamura bundles.

You will find the bundles under ./bundles and some libraries under ./libraries.


