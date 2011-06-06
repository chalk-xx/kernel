#!/bin/bash

#Sakai 3 Demo
export K2_TAG="HEAD"
export UX_TAG="HEAD"

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
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=512m"
export JAVA_OPTS="-server -Xmx512m -XX:MaxPermSize=128m -Djava.awt.headless=true"
export K2_OPTS="-server -Xmx1024m -XX:MaxPermSize=256m -Djava.awt.headless=true"
BUILD_DATE=`date "+%D %R"`

# ensure logs directory exists
if [ ! -d $BUILD_DIR/logs ]
then
	mkdir $BUILD_DIR/logs
fi

# shutdown all running instances
killall -9 java

# Exit immediately if a simple command exits with a non-zero status
set -o errexit

# clean previous builds
cd $BUILD_DIR
rm -rf 3akai-ux
rm -rf sakai3
rm -rf ~/.m2/repository/org/sakaiproject

# clean mysql database
echo "Cleaning MySQL..."
mysql -u sakaiuser << EOSQL
drop database if exists nakamura;
create database nakamura default character set 'utf8';
exit
EOSQL

# build 3akai ux
echo "Building 3akai-ux@$UX_TAG..."
cd $BUILD_DIR
mkdir 3akai-ux
cd 3akai-ux
git clone -q git://github.com/sakaiproject/3akai-ux.git
cd 3akai-ux
git checkout -b "build-$UX_TAG" $UX_TAG
mvn -B -e clean install

# build sakai 3
echo "Building nakamura@$K2_TAG..."
cd $BUILD_DIR
mkdir sakai3
cd sakai3
git clone -q git://github.com/sakaiproject/nakamura.git
cd nakamura
git checkout -b "build-$K2_TAG" $K2_TAG
mvn -B -e clean install
#install optional mysql driver
cd contrib/mysql-jdbc
mvn -B -e clean install
cd ../../app/
perl -pwi -e 's/<startLevel level="1">/<startLevel level="1"><bundle><groupId>org\.sakaiproject\.nakamura<\/groupId><artifactId>org\.sakaiproject\.nakamura\.mysqljdbc<\/artifactId><version>0.11-SNAPSHOT<\/version><\/bundle>/gi' src/main/bundles/list.xml
mvn -B -e clean install
cd ..

# start sakai 3 instance
echo "Starting sakai3 instance..."
cd app/target/
K2_ARTIFACT=`find . -name "org.sakaiproject.nakamura.app*[^sources].jar"`
# configure ServerProtectionServiceImpl
mkdir -p sling/config/org/sakaiproject/nakamura/http/usercontent
echo 'service.pid="org.sakaiproject.nakamura.http.usercontent.ServerProtectionServiceImpl"' > sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.paths=["/dev","/devwidgets","/system"]' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.postwhitelist=["/system/console"]' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.exact.paths=["/","/index.html"]' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.anonpostwhitelist=["/system/userManager/user.create","/system/batch"]' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'disable.protection.for.dev.mode=B"false"' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.secret="shhhhh"' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'untrusted.contenturl="http://sakai3-demo.uits.indiana.edu:8082"' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.hosts=["http://sakai3-demo.uits.indiana.edu:8080"]' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
echo 'trusted.referer=["/","http://sakai3-demo.uits.indiana.edu:8080"]' >> sling/config/org/sakaiproject/nakamura/http/usercontent/ServerProtectionServiceImpl.config
#configure JDBC connector
mkdir -p sling/config/org/sakaiproject/nakamura/lite/storage/jdbc
echo 'service.pid="org.sakaiproject.nakamura.lite.storage.jdbc.JDBCStorageClientPool"' > sling/config/org/sakaiproject/nakamura/lite/storage/jdbc/JDBCStorageClientPool.config
echo 'jdbc-driver="com.mysql.jdbc.Driver"' >> sling/config/org/sakaiproject/nakamura/lite/storage/jdbc/JDBCStorageClientPool.config
echo 'jdbc-url="jdbc:mysql://localhost/nakamura?autoReconnectForPools=true"' >> sling/config/org/sakaiproject/nakamura/lite/storage/jdbc/JDBCStorageClientPool.config
echo 'username="sakaiuser"' >> sling/config/org/sakaiproject/nakamura/lite/storage/jdbc/JDBCStorageClientPool.config
echo 'password="ironchef"' >> sling/config/org/sakaiproject/nakamura/lite/storage/jdbc/JDBCStorageClientPool.config
java $K2_OPTS -jar $K2_ARTIFACT -p 8080 -f - > $BUILD_DIR/logs/sakai3-run.log.txt 2>&1 &

# final cleanup
cd $BUILD_DIR
# rm -rf ~/.m2/repository/org/sakaiproject

# run nakamura integration tests
echo "Sleeping ten minutes before running integration tests..."
sleep 600
echo "Running integration tests..."
cd $BUILD_DIR/sakai3/nakamura
date > $BUILD_DIR/logs/sakai3-integration-tests.log.txt
uname -a >> $BUILD_DIR/logs/sakai3-integration-tests.log.txt 2>&1
java -version >> $BUILD_DIR/logs/sakai3-integration-tests.log.txt 2>&1
./tools/runalltests.rb >> $BUILD_DIR/logs/sakai3-integration-tests.log.txt 2>&1
