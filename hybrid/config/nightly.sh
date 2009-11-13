#Sakai 2+3 Hybrid Nightly
# don't forget to trust the svn certificate permanently: svn info https://source.sakaiproject.org/svn
# and svn info https://source.caret.cam.ac.uk/camtools

# environment
source /etc/profile
export JAVA_HOME=/Library/Java/Home
export PATH=$JAVA_HOME/bin:${PATH}
export MAVEN_HOME=/usr/local/apache-maven-2.2.1
export M2_HOME=/usr/local/apache-maven-2.2.1
export PATH=$MAVEN_HOME/bin:${PATH}
export MAVEN_OPTS="-Xmx1024m -XX:MaxPermSize=256m"
export JAVA_OPTS="-server -Xmx1024m -XX:MaxPermSize=512m -Djava.awt.headless=true -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCTimeStamps -Dsun.lang.ClassLoader.allowArraySyntax=true -Dsakai.demo=true -Dsakai.cookieName=SAKAI2SESSIONID"
BUILD_DATE=`date "+%D %R"`
BUILD_DIR="/Users/hybrid"
cd $BUILD_DIR

# shutdown all running instances
killall -9 java

# clean previous builds
rm -rf sakai
rm -rf kernel
rm -rf sakai2-demo
rm -rf sling
rm -rf sakai3
rm -rf ~/.m2/repository/

# build sling/trunk
# not needed as long as hudson builds are getting deployed to apache-snapshot repo
# org.apache.sling.launchpad.base-2.0.5-SNAPSHOT-app.jar cannot be found - build sling
cd $BUILD_DIR
svn checkout -q http://svn.apache.org/repos/asf/sling/trunk sling
cd sling
mvn clean install -Dmaven.test.skip=true
rm -rf sling

# build sakai 3
cd $BUILD_DIR
mkdir sakai3
cd sakai3
git clone -q git://github.com/ieb/open-experiments.git
cd open-experiments/slingtests/osgikernel/
mvn clean install -Dmaven.test.skip=true

# start sakai 3 instance
cd app/target/
java -jar org.sakaiproject.kernel.app-0.1-SNAPSHOT.jar -f - > $BUILD_DIR/sakai3/log.txt 2>&1 &

# untar tomcat
cd $BUILD_DIR
tar -xzf apache-tomcat-5.5.26.tar.gz 
mv apache-tomcat-5.5.26 sakai2-demo
mkdir sakai2-demo/sakai

# build kernel 1
svn checkout -q https://source.sakaiproject.org/svn/kernel/trunk/ kernel
cd kernel
mvn clean install -Dmaven.test.skip=true
cd ..
rm -rf kernel

# build sakai 2
svn checkout -q https://source.sakaiproject.org/svn/sakai/trunk/ sakai
cd sakai/
REPO_REV=`svn info|grep Revision`
# SAK-17223 K2AuthenticationFilter
rm -rf login/
svn checkout -q https://source.sakaiproject.org/svn/login/branches/SAK-17223/ login
# KERN-360 Servlet and TrustedLoginFilter RESTful services
cp -R $BUILD_DIR/sakai3/open-experiments/hybrid .
#sed -i 's/<\/modules>/<module>hybrid<\/module><\/modules>/gi' pom.xml 
# work around for broken sed on some systems
perl -pwi -e 's/<\/modules>/<module>hybrid<\/module><\/modules>/gi' pom.xml
#
mvn clean install sakai:deploy -Dmaven.test.skip=true -Dmaven.tomcat.home=$BUILD_DIR/sakai2-demo
cd ..
rm -rf sakai

# configure sakai 2 instance
cp -f server.xml sakai2-demo/conf/server.xml 
echo "ui.service = trunk+SAK-17223+KERN-360 on HSQLDB" >> sakai2-demo/sakai/sakai.properties
echo "version.sakai = $REPO_REV" >> sakai2-demo/sakai/sakai.properties
echo "version.service = Built: $BUILD_DATE" >> sakai2-demo/sakai/sakai.properties
#echo "smtp=localhost" >> sakai2-demo/sakai/sakai.properties
#echo "smtp@org.sakaiproject.email.api.EmailService=localhost" >> sakai2-demo/sakai/sakai.properties
#echo "smtp.enabled = true" >> sakai2-demo/sakai/sakai.properties
#echo "smtp.port=8028" >> sakai2-demo/sakai/sakai.properties
echo "serverName=nightly2.sakaiproject.org" >> sakai2-demo/sakai/sakai.properties
echo "webservices.allowlogin=true" >> sakai2-demo/sakai/sakai.properties
echo "webservice.portalsecret=nightly" >> sakai2-demo/sakai/sakai.properties
echo "samigo.answerUploadRepositoryPath= /tmp/sakai2-hybrid/" >> sakai2-demo/sakai/sakai.properties
# enable SAK-17223 K2AuthenticationFilter
echo "top.login=false" >> sakai2-demo/sakai/sakai.properties
echo "container.login=true" >> sakai2-demo/sakai/sakai.properties
echo "login.k2.authentication=true" >> sakai2-demo/sakai/sakai.properties
echo "login.k2.authentication.vaildateUrl=http://localhost:8080/var/cluster/user.cookie.json?c=" >> sakai2-demo/sakai/sakai.properties

# start sakai 2 tomcat
sakai2-demo/bin/startup.sh 

# final cleanup
cd $BUILD_DIR
rm -rf ~/.m2/repository/
