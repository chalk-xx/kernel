#!/bin/sh
# This script versions all SNAPSHOTS that are not part of the core project.

repo=~/.m2/repository

function install {
   if [ -f $repo/org/apache/sling/${1}/${2}-${3}/${1}-${2}-${3}.jar ]
   then
     echo Version ${1}-${2}-${3} exists.
   else
     pushd tools
     mvn install:install-file -DgroupId=org.apache.sling -DartifactId=${1} -Dversion=${2}-${3} -Dpackaging=jar -Dfile=$repo/org/apache/sling/${1}/${2}-SNAPSHOT/${1}-${2}-SNAPSHOT.jar
     popd 
   fi
}




set -o nounset
set -o errexit
cversion=$1
capture=$2
set +o errexit
hascommits=`git status -uno | grep -c "nothing to commit"`
if [[ $hascommits -ne 1 ]]
then
   git status
   echo "Please commit uncommitted work before fixing a release version "
   exit -1
fi
set -o errexit

listofpoms=`find . -exec grep -l SNAPSHOT {} \;| egrep -v ".git|do_release.sh|target|binary/release|uxloader/src/main/resources|last-release|cachedir"`
listofpomswithversion=`grep -l $cversion-SNAPSHOT $listofpoms`
listofpomstoedit=`grep -l SNAPSHOT $listofpoms | egrep -v "sandbox/|tools/|/not.an.image|hybrid/|nightly/|contrib/nyu/|contrib/ucb/|contrib/uc/|app/pom.xml"`


set +o errexit
git branch -D versionchange
set -o errexit
git branch versionchange
git co versionchange

echo "Creating Release"
for i in $listofpomswithversion
do
  sed "s/$cversion-SNAPSHOT/$cversion/" $i > $i.new
  mv $i.new $i
done

git commit -a -m "Version Change in branch"

for i in $listofpomstoedit
do
  sed "s/-SNAPSHOT/-$capture/" $i > $i.new
  mv $i.new $i
done


git stash save

echo "Remaining SNAPSHOT versions in the release"
echo "=================================================="
grep -C5 SNAPSHOT $listofpomstoedit
echo "=================================================="



git co master
git branch -D versionchange

git stash apply 

git diff > last-release/captured-snapshots.patch

versionstocapture=`git diff | awk '/artifactId/ { artifact=$1 }; /groupId/ { group=$1 }; /SNAPSHOT/ { if ( artifact ) print substr(group,10,length(group)-19) ":" substr(artifact,13,length(artifact)-25) ":"  substr($2,10,length($2)-28) }; ' | sort -u `

for i in $versionstocapture
do
  echo $i
   group=`echo $i | cut -d':' -f1`
   artifact=`echo $i | cut -d':' -f2`
   version=`echo $i | cut -d':' -f3`
   install "$artifact" "$version" "$capture"
done


  

