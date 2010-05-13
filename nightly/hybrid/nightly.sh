#!/bin/bash

#Sakai 2+3 Hybrid Nightly
# don't forget to trust the svn certificate permanently: svn info https://source.sakaiproject.org/svn
# and svn info https://source.caret.cam.ac.uk/camtools

export K2_TAG="0.4"
export S2_TAG="tags/sakai-2.7.0-b06"
export K2_ARTIFACT="org.sakaiproject.nakamura.app-0.4-SNAPSHOT.jar"

# Treat unset variables as an error when performing parameter expansion
set -o nounset

# environment
export PATH=/usr/local/bin:$PATH
export BUILD_DIR="/home/hybrid"
export JAVA_HOME=/opt/jdk1.6.0_17
export PATH=$JAVA_HOME/bin:${PATH}
export MAVEN_HOME=/usr/local/apache-maven-2.2.1
export M2_HOME=/usr/local/apache-maven-2.2.1
export PATH=$MAVEN_HOME/bin:${PATH}
export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m"
export JAVA_OPTS="-server -Xmx1024m -XX:MaxPermSize=512m -Djava.awt.headless=true -Dsun.lang.ClassLoader.allowArraySyntax=true -Dsakai.demo=true -Dsakai.cookieName=SAKAI2SESSIONID"
export K2_OPTS="-server -Xmx512m -XX:MaxPermSize=128m -Djava.awt.headless=true"
BUILD_DATE=`date "+%D %R"`

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`/"$link"
    fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`
PRGDIR=`pwd "$PRGDIR"`

# ensure logs directory exists
if [ ! -d $BUILD_DIR/logs ]
then
	mkdir -p $BUILD_DIR/logs
fi

# shutdown all running instances
killall -9 java

# Exit immediately if a simple command exits with a non-zero status
set -o errexit

# clean previous builds
cd $BUILD_DIR
if [ $1 == "clean" ]
then
    echo "Starting clean build..."
    rm -rf sakai
    rm -rf sakai2-demo
    rm -rf sakai3
    rm -rf ~/.m2/repository/org/sakaiproject
else
    echo "Starting incremental build..."
fi

# build sakai 3
cd $BUILD_DIR
mkdir -p sakai3
cd sakai3
if [ -f .lastbuild ]
then
    echo "Skipping build nakamura@$K2_TAG..."
else
    echo "Building nakamura@$K2_TAG..."
    git clone -q git://github.com/ieb/open-experiments.git
    cd open-experiments/slingtests/osgikernel/
    git checkout -b $K2_TAG
    mvn -B -e clean install -Dmaven.test.skip=true
    date > .lastbuild
fi

# start sakai 3 instance
echo "Starting sakai3 instance..."
cd app/target/
mkdir -p sling/config/org/sakaiproject/nakamura/proxy
echo 'port=I"8080"' > sling/config/org/sakaiproject/nakamura/proxy/TrustedLoginTokenProxyPreProcessor.config
echo 'sharedSecret="e2KS54H35j6vS5Z38nK40"' >> sling/config/org/sakaiproject/nakamura/proxy/TrustedLoginTokenProxyPreProcessor.config
echo 'service.pid="org.sakaiproject.nakamura.proxy.TrustedLoginTokenProxyPreProcessor"' >> sling/config/org/sakaiproject/nakamura/proxy/TrustedLoginTokenProxyPreProcessor.config
java $K2_OPTS -jar $K2_ARTIFACT -p 8008 -f - > $BUILD_DIR/logs/sakai3-run.log.txt 2>&1 &

# build sakai 2
cd $BUILD_DIR
if [ -f $BUILD_DIR/sakai/.lastbuild ]
then
    echo "Skipping build sakai2/$S2_TAG..."
else
    echo "Building sakai2/$S2_TAG..."
    # untar tomcat
    tar -xzf apache-tomcat-5.5.26.tar.gz 
    mv apache-tomcat-5.5.26 sakai2-demo
    mkdir -p sakai2-demo/sakai
    svn checkout -q "https://source.sakaiproject.org/svn/sakai/$S2_TAG" sakai
    cd sakai/
    REPO_REV=`svn info|grep Revision`
    # SAK-17223 K2AuthenticationFilter
    rm -rf login/
    svn checkout -q https://source.sakaiproject.org/svn/login/branches/SAK-17223-2.7 login
    # SAK-17222 K2UserDirectoryProvider
    rm -rf providers
    svn checkout -q https://source.sakaiproject.org/svn/providers/branches/SAK-17222-2.7 providers
    # KERN-360 Servlet and TrustedLoginFilter RESTful services
    cp -R $BUILD_DIR/sakai3/open-experiments/hybrid .
    find hybrid -name pom.xml -exec perl -pwi -e 's/2\.8-SNAPSHOT/2\.7-SNAPSHOT/g' {} \;
    perl -pwi -e 's/<\/modules>/<module>hybrid<\/module><\/modules>/gi' pom.xml
    mvn -B -e clean install sakai:deploy -Dmaven.test.skip=true -Dmaven.tomcat.home=$BUILD_DIR/sakai2-demo
    # configure sakai 2 instance
    cd $BUILD_DIR
    cp -f server.xml sakai2-demo/conf/server.xml 
    echo "ui.service = trunk+SAK-17223+KERN-360 on HSQLDB" >> sakai2-demo/sakai/sakai.properties
    echo "version.sakai = $REPO_REV" >> sakai2-demo/sakai/sakai.properties
    echo "version.service = Built: $BUILD_DATE" >> sakai2-demo/sakai/sakai.properties
    echo "serverName=nightly2.sakaiproject.org" >> sakai2-demo/sakai/sakai.properties
    echo "webservices.allowlogin=true" >> sakai2-demo/sakai/sakai.properties
    echo "webservice.portalsecret=nightly" >> sakai2-demo/sakai/sakai.properties
    echo "samigo.answerUploadRepositoryPath= /tmp/sakai2-hybrid/" >> sakai2-demo/sakai/sakai.properties
    # enable SAK-17223 K2AuthenticationFilter
    echo "top.login=false" >> sakai2-demo/sakai/sakai.properties
    echo "container.login=true" >> sakai2-demo/sakai/sakai.properties
    echo "login.k2.authentication=true" >> sakai2-demo/sakai/sakai.properties
    echo "login.k2.authentication.vaildateUrl=http://localhost:8008/var/cluster/user.cookie.json?c=" >> sakai2-demo/sakai/sakai.properties
    # declare shared secret for trusted login from K2
    echo "org.sakaiproject.util.TrustedLoginFilter.sharedSecret=e2KS54H35j6vS5Z38nK40" >> sakai2-demo/sakai/sakai.properties
    echo "org.sakaiproject.util.TrustedLoginFilter.safeHosts=localhost" >> sakai2-demo/sakai/sakai.properties
    date > $BUILD_DIR/sakai/.lastbuild
fi

# start sakai 2 tomcat
echo "Starting sakai2 instance..."
cd $BUILD_DIR/sakai2-demo
./bin/startup.sh 
