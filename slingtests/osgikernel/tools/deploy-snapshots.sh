#!/bin/sh
# This script deploys snapshots to the local repo based on the current snapshot version and creates a tarball of the repo.

version=$1
repo=/Users/ieb/.m2/repository
function install {
   mvn install:install-file -DgroupId=org.apache.sling -DartifactId=${1} -Dversion=${2}-${3} -Dpackaging=jar -Dfile=$repo/org/apache/sling/${1}/${2}-SNAPSHOT/${1}-${2}-SNAPSHOT.jar
}
#install "org.apache.sling.extensions.webconsolebranding" "0.0.1" $version
#install "org.apache.sling.jcr.webconsole" "1.0.0" $version
#install "org.apache.sling.commons.json" "2.0.5" $version
#install "org.apache.sling.jcr.base" "2.0.5" $version
#install "org.apache.sling.commons.auth" "0.9.0" $version
#install "org.apache.sling.engine" "2.0.7" $version
#install "org.apache.sling.systemstatus" "0.9.0" $version
#install "org.apache.sling.httpauth" "2.0.5" $version
#install "org.apache.sling.openidauth" "0.9.1" $version
#install "org.apache.sling.servlets.get" "2.0.9" $version
#install "org.apache.sling.servlets.post" "2.0.5" $version
#install "org.apache.sling.jcr.contentloader" "2.0.5" $version
#install "org.apache.sling.jcr.resource" "2.0.7" $version
install "org.apache.sling.jcr.jackrabbit.server" "2.0.5" $version
#install "org.apache.sling.jcr.jackrabbit.usermanager" "2.0.3" $version
pushd $repo
vsearch="*${version}*"
files=`find . -type f -name $vsearch | grep -v .asc `
for i in $files
do 
openssl sha1 $i > $i.asc
done
files=`find . -type d -name $vsearch | grep -v .asc `
tar cvzf /tmp/repo.tgz $files
popd
mv /tmp/repo.tgz .



