#!/bin/bash

#Sakai 2+3 Hybrid Nightly
# don't forget to trust the svn certificate permanently: svn info https://source.sakaiproject.org/svn

export K2_TAG="HEAD"
export S2_TAG="tags/sakai-2.8.0"
export UX_SRC="git://github.com/sakaiproject/3akai-ux.git"
export UX_TAG="HEAD"
export HYBRID_TAG="branches/hybrid-1.1.x"
export K2_HTTP_PORT="8080"
export S2_HTTP_PORT="8880"
export S2_AJP_PORT="8889"
export HTTPD_PORT="8088"

# Treat unset variables as an error when performing parameter expansion
set -o nounset

# environment
export PATH=/usr/local/bin:$PATH
export BUILD_DIR="/home/hybrid"
export JAVA_HOME=/opt/jdk1.6.0_22
export PATH=$JAVA_HOME/bin:${PATH}
export MAVEN_HOME=/usr/local/apache-maven-2.2.1
export M2_HOME=/usr/local/apache-maven-2.2.1
export PATH=$MAVEN_HOME/bin:${PATH}
export MAVEN_OPTS="-Xmx512m -XX:MaxPermSize=256m"
export JAVA_OPTS="-server -Xmx1024m -XX:MaxPermSize=512m -Djava.awt.headless=true -Dsun.lang.ClassLoader.allowArraySyntax=true -Dorg.apache.jasper.compiler.Parser.STRICT_QUOTE_ESCAPING=false -Dsakai.demo=true -Dsakai.cookieName=SAKAI2SESSIONID"
export K2_OPTS="-server -Xmx1024m -XX:MaxPermSize=256m -Djava.awt.headless=true"
BUILD_DATE=`date "+%D %R"`

# get some shell scripting setup out of the way...
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
if [ $# -gt 0 ]
then
    if [ $1 == "clean" ]
    then
        echo "Starting clean build..."
        rm -rf sakai
        rm -rf sakai2-demo
        rm -rf 3akai-ux
        rm -rf sakai3
        # rm -rf ~/.m2/repository/org/sakaiproject
    else
        echo "Starting incremental build..."
    fi
else
    echo "Starting incremental build..."
fi

# clean mysql database
echo "Cleaning MySQL..."
mysql -u sakaiuser << EOSQL
drop database nakamura;
create database nakamura default character set 'utf8';
exit
EOSQL

# build 3akai-ux
cd $BUILD_DIR
mkdir -p 3akai-ux
cd 3akai-ux
if [ -f .lastbuild ]
then
    echo "Skipping build 3akai-ux@$UX_TAG..."
else
    echo "Building 3akai-ux@$UX_TAG..."
    git clone -q "$UX_SRC"
    cd 3akai-ux
    git checkout -b "build-$UX_TAG" $UX_TAG
    # enable My Sakai 2 Sites widget
    # "personalportal":false --> "personalportal":true
    # perl -pwi -e 's/"personalportal"\s*\:\s*false/"personalportal"\:true/gi' devwidgets/mysakai2/config.json
    # "showSakai2" : false --> "showSakai2" : true
    perl -pwi -e 's/showSakai2\s*\:\s*false/showSakai2 \: true/gi' dev/configuration/config.js
    # "useLiveSakai2Feeds" : false --> "useLiveSakai2Feeds" : true
    perl -pwi -e 's/useLiveSakai2Feeds\s*\:\s*false/useLiveSakai2Feeds \: true/gi' dev/configuration/config.js
    mvn -B -e clean install
    date > ../.lastbuild
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
    git clone -q git://github.com/sakaiproject/nakamura.git
    cd nakamura
    git checkout -b "build-$K2_TAG" $K2_TAG
    mvn -B -e clean install
    date > .lastbuild
fi

# start sakai 3 instance
echo "Starting sakai3 instance..."
cd app/target/
K2_ARTIFACT=`find . -name "org.sakaiproject.nakamura.app*[^sources].jar"`
# configure TrustedLoginTokenProxyPreProcessor
mkdir -p sling/config/org/sakaiproject/nakamura/proxy
echo 'service.pid="org.sakaiproject.nakamura.proxy.TrustedLoginTokenProxyPreProcessor"' > sling/config/org/sakaiproject/nakamura/proxy/TrustedLoginTokenProxyPreProcessor.config
echo 'hostname="localhost"' >> sling/config/org/sakaiproject/nakamura/proxy/TrustedLoginTokenProxyPreProcessor.config
echo "port=I\"$HTTPD_PORT\"" >> sling/config/org/sakaiproject/nakamura/proxy/TrustedLoginTokenProxyPreProcessor.config
echo 'sharedSecret="e2KS54H35j6vS5Z38nK40"' >> sling/config/org/sakaiproject/nakamura/proxy/TrustedLoginTokenProxyPreProcessor.config
# configure ServerProtectionServiceImpl
mkdir -p sling/config/org/sakaiproject/nakamura/http/usercontent
echo 'service.pid="org.sakaiproject.nakamura.http.usercontent.ServerProtectionServiceImpl"' > sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.paths=["/dev","/devwidgets","/system"]' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.postwhitelist=["/system/console"]' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.exact.paths=["/","/index.html"]' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.anonpostwhitelist=["/system/userManager/user.create","/system/batch"]' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'disable.protection.for.dev.mode=B"false"' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.secret="shhhhh"' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'untrusted.contenturl="http://sakai3-nightly.uits.indiana.edu:8082"' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo "trusted.hosts=[\"http://sakai3-nightly.uits.indiana.edu:$HTTPD_PORT\"]" >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo "trusted.referer=[\"/\",\"http://sakai3-nightly.uits.indiana.edu:$HTTPD_PORT\"]" >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
#configure JDBC connector
mkdir -p sling/config/org/sakaiproject/nakamura/lite/storage/jdbc
echo 'service.pid="org.sakaiproject.nakamura.lite.storage.jdbc.JDBCStorageClientPool"' > sling/config/org/sakaiproject/nakamura/lite/storage/jdbc/JDBCStorageClientPool.config
echo 'jdbc-driver="com.mysql.jdbc.Driver"' >> sling/config/org/sakaiproject/nakamura/lite/storage/jdbc/JDBCStorageClientPool.config
echo 'jdbc-url="jdbc:mysql://localhost/nakamura?autoReconnectForPools=true"' >> sling/config/org/sakaiproject/nakamura/lite/storage/jdbc/JDBCStorageClientPool.config
echo 'username="sakaiuser"' >> sling/config/org/sakaiproject/nakamura/lite/storage/jdbc/JDBCStorageClientPool.config
echo 'password="ironchef"' >> sling/config/org/sakaiproject/nakamura/lite/storage/jdbc/JDBCStorageClientPool.config
java $K2_OPTS -jar $K2_ARTIFACT -f - > $BUILD_DIR/logs/sakai3-run.log.txt 2>&1 &

# build sakai 2
cd $BUILD_DIR
if [ -f $BUILD_DIR/sakai/.lastbuild ]
then
    echo "Skipping build sakai2/$S2_TAG..."
else
    echo "Building sakai2/$S2_TAG..."
    # untar tomcat
    tar -xzf apache-tomcat-5.5.33.tar.gz
    mv apache-tomcat-5.5.33 sakai2-demo
    mkdir -p sakai2-demo/sakai
    svn checkout -q "https://source.sakaiproject.org/svn/sakai/$S2_TAG" sakai
    cd sakai/
    REPO_REV=`svn info|grep Revision`
    # # SAK-17223 K2AuthenticationFilter
    # rm -rf login/
    # svn checkout -q https://source.sakaiproject.org/svn/login/branches/SAK-17223-2.7 login
    # SAK-17222 NakamuraUserDirectoryProvider
    # rm -rf providers
    # svn checkout -q https://source.sakaiproject.org/svn/providers/branches/SAK-17222-2.7 providers
    # enable NakamuraUserDirectoryProvider
    perl -pwi -e 's/<\/beans>/\t<bean id="org.sakaiproject.user.api.UserDirectoryProvider"\n\t\tclass="org.sakaiproject.provider.user.NakamuraUserDirectoryProvider"\n\t\tinit-method="init">\n\t\t<property name="threadLocalManager">\n\t\t\t<ref bean="org.sakaiproject.thread_local.api.ThreadLocalManager" \/>\n\t\t<\/property>\n\t\t<property name="serverConfigurationService">\n\t\t\t<ref bean="org.sakaiproject.component.api.ServerConfigurationService" \/>\n\t\t<\/property>\n\t<\/bean>\n<\/beans>/gi' providers/component/src/webapp/WEB-INF/components.xml
    rm -f providers/component/src/webapp/WEB-INF/components-demo.xml
    mvn -B -e clean install sakai:deploy -Dmaven.tomcat.home=$BUILD_DIR/sakai2-demo
    # add hybrid webapp module
    echo "Building hybrid/$HYBRID_TAG"
    svn checkout -q https://source.sakaiproject.org/svn/hybrid/$HYBRID_TAG hybrid
    cd hybrid
    mvn -B -e clean install sakai:deploy -Dmaven.tomcat.home=$BUILD_DIR/sakai2-demo
    # configure sakai 2 instance
    cd $BUILD_DIR
    # change default tomcat listener port numbers
    perl -pwi -e "s/\<Connector\s+port\s*\=\s*\"8080\"/\<Connector port\=\"$S2_HTTP_PORT\"/gi" sakai2-demo/conf/server.xml
    perl -pwi -e "s/\<Connector\s+port\s*\=\s*\"8009\"/\<Connector port\=\"$S2_AJP_PORT\"/gi" sakai2-demo/conf/server.xml
    # sakai.properties
    echo "ui.service = $S2_TAG + $HYBRID_TAG on HSQLDB" >> sakai2-demo/sakai/sakai.properties
    echo "version.sakai = $REPO_REV" >> sakai2-demo/sakai/sakai.properties
    echo "version.service = Built: $BUILD_DATE" >> sakai2-demo/sakai/sakai.properties
    echo "serverName=sakai3-nightly.uits.indiana.edu" >> sakai2-demo/sakai/sakai.properties
    echo "webservices.allowlogin=true" >> sakai2-demo/sakai/sakai.properties
    echo "webservice.portalsecret=nightly" >> sakai2-demo/sakai/sakai.properties
    echo "samigo.answerUploadRepositoryPath=/tmp/sakai2-hybrid/" >> sakai2-demo/sakai/sakai.properties
    # enable SAK-17223 NakamuraAuthenticationFilter
    echo "top.login=false" >> sakai2-demo/sakai/sakai.properties
    echo "container.login=true" >> sakai2-demo/sakai/sakai.properties
    echo "org.sakaiproject.login.filter.NakamuraAuthenticationFilter.enabled=true" >> sakai2-demo/sakai/sakai.properties
    echo "org.sakaiproject.login.filter.NakamuraAuthenticationFilter.validateUrl=http://localhost:$K2_HTTP_PORT/var/cluster/user.cookie.json?c=" >> sakai2-demo/sakai/sakai.properties
    # configure SAK-17222 NakamuraUserDirectoryProvider
    echo "org.sakaiproject.provider.user.NakamuraUserDirectoryProvider.validateUrl=http://localhost:$K2_HTTP_PORT/var/cluster/user.cookie.json?c=" >> sakai2-demo/sakai/sakai.properties
    echo "x.sakai.token.localhost.sharedSecret=default-setting-change-before-use" >> sakai2-demo/sakai/sakai.properties
    # declare shared secret for trusted login from nakamura
    echo "org.sakaiproject.hybrid.util.TrustedLoginFilter.sharedSecret=e2KS54H35j6vS5Z38nK40" >> sakai2-demo/sakai/sakai.properties
    echo "org.sakaiproject.hybrid.util.TrustedLoginFilter.safeHosts=localhost;127.0.0.1;0:0:0:0:0:0:0:1%0;129.79.26.127" >> sakai2-demo/sakai/sakai.properties
    # enabled Basic LTI provider
    echo "imsblti.provider.enabled=true" >> sakai2-demo/sakai/sakai.properties
    echo "imsblti.provider.allowedtools=sakai.forums:sakai.messages:sakai.synoptic.messagecenter:sakai.poll:sakai.profile:sakai.profile2:sakai.announcements:sakai.synoptic.announcement:sakai.assignment.grades:sakai.summary.calendar:sakai.schedule:sakai.chat:sakai.dropbox:sakai.resources:sakai.gradebook.tool:sakai.help:sakai.mailbox:sakai.news:sakai.podcasts:sakai.postem:sakai.site.roster:sakai.rwiki:sakai.syllabus:sakai.singleuser:sakai.samigo:sakai.sitestats" >> sakai2-demo/sakai/sakai.properties
    echo "imsblti.provider.12345.secret=secret" >> sakai2-demo/sakai/sakai.properties
    echo "webservices.allow=.+" >> sakai2-demo/sakai/sakai.properties
    # enable debugging for hybrid related code
    echo "log.config.count=6" >> sakai2-demo/sakai/sakai.properties
    echo "log.config.1 = ALL.org.sakaiproject.log.impl" >> sakai2-demo/sakai/sakai.properties
    echo "log.config.2 = WARN.org.sakaiproject" >> sakai2-demo/sakai/sakai.properties
    echo "log.config.3 = DEBUG.org.sakaiproject.provider.user" >> sakai2-demo/sakai/sakai.properties
    echo "log.config.4 = DEBUG.org.sakaiproject.login" >> sakai2-demo/sakai/sakai.properties
    echo "log.config.5 = DEBUG.org.sakaiproject.hybrid" >> sakai2-demo/sakai/sakai.properties
    echo "log.config.6 = DEBUG.org.sakaiproject.blti" >> sakai2-demo/sakai/sakai.properties
    date > $BUILD_DIR/sakai/.lastbuild
fi

# start sakai 2 tomcat
echo "Starting sakai2 instance..."
cd $BUILD_DIR/sakai2-demo
./bin/startup.sh 

# run nakamura integration tests
echo "Running integration tests..."
cd $BUILD_DIR/sakai3/nakamura
date > $BUILD_DIR/logs/sakai3-integration-tests.log.txt
uname -a >> $BUILD_DIR/logs/sakai3-integration-tests.log.txt 2>&1
java -version >> $BUILD_DIR/logs/sakai3-integration-tests.log.txt 2>&1
./tools/runalltests.rb >> $BUILD_DIR/logs/sakai3-integration-tests.log.txt 2>&1
