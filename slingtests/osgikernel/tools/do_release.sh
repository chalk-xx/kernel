#!/bin/sh

curl -f http://localhost:8080/index.html 2> /dev/null
if [[ $? -ne 7 ]]
then
   echo "There is already a server on port 8080, please stop it before attempting to perform a release" 
   portline=`lsof | grep IPv | grep http-alt`
   processnum=`echo $portline | cut -f2 -d ' '`
   echo $portline
   ps auxwww | grep $processnum
   echo kill $processnum
   exit -1
fi

if [ -f last-release/stage3 ]
then
   echo "Remove last-release/stage3 and commit the change if necessary to perform a release"
   exit -1
fi

set -o nounset
set -o errexit
cversion=$1
nversion=$2
ignoreTests=${3:-"__none__"}
mkdir -p last-release
uname -a > last-release/who

if [[ -f last-release/stage1 ]]
then
   echo "Release has been built, continuing ... (to start again remove last-release/stage1) "
else
  listofpoms=`find . -name pom.xml | grep -v target`
  listofpomswithversion=`grep -l $cversion-SNAPSHOT $listofpoms`
  set +o errexit
  hascommits=`git status -uno | grep -c "nothing to commit"`
  if [[ $hascommits -ne 1 ]]
  then
    git status
    echo "Please commit uncommitted work before performing a release "
    exit -1
  fi
  set -o errexit
  
  
  echo "Creating Release"
  for i in $listofpomswithversion
  do
    sed "s/$cversion-SNAPSHOT/$cversion/" $i > $i.new
    mv $i.new $i
  done
  git diff > last-release/changeversion.diff
  
  echo "Remaining SNAPSHOT versions in the release"
  echo "=================================================="
  grep -C5 SNAPSHOT $listofpoms
  echo "=================================================="
  
  rm -rf ~/.m2/repository/org/sakaiproject/kernel
  mvn clean install  | tee last-release/build.log 
  date > last-release/stage1

  echo "Build complete, preparing startup "
fi

if [[ -f last-release/stage2 ]]
then
   echo "Integration tests complete, continuing ... (to start again remove last-release/stage2 )"
else 
  rm -rf sling
  has_32_bit=`java -help | grep -c "\-d32"`
  if [[ $has_32_bit == "1" ]]
  then
    d32="-d32"
  else
    d32=""
  fi
  
  
  echo "Starting server, log in last-release/run.log"
  java  $d32 -XX:MaxPermSize=128m -Xmx512m -server -Dcom.sun.management.jmxremote -jar app/target/org.sakaiproject.kernel.app-$cversion.jar -f - 1> last-release/run.log 2>&1 & 
  pid=`ps auxwww | grep java | grep  app/target/org.sakaiproject.kernel.app-0.1.jar | cut -c7-15`
  tsleep=30
  retries=0
  while [[ $tsleep -ne 0 ]]
  do
    if [[ $retries -gt 31 ]]
    then
	  echo "Too Many retries attempted trying to start K2 for testing, server left running."
          exit -1
    fi
    echo "Sleeping for $tsleep seconds while server starts ... "
    sleep $tsleep
    set +o errexit
    curl -f http://localhost:8080/index.html > /dev/null
    if [[ $? -ne 0 ]]
    then
      tsleep=10
    else
      tsleep=0
    fi
    set -o errexit
    let retries=retries+1
  done
  sleep 5
  
  echo "Server Started, running integration tests, log in last-release/integration.log"
  TESTS=`find . -name testall.sh`
  (
  set -o errexit
  for i in $TESTS
  do
          pushd `dirname $i`
          ./testall.sh  
          popd
  done 
  set +o errexit
  ) > last-release/integration.log
  date > last-release/stage2
fi

egrep -v "$ignoreTests" last-release/integration.log > last-release/integration-check.log


failures=` grep -v "0 failures" last-release/integration-check.log  | grep -v osgikernel | wc -l`
errors=` grep -v "0 errors" last-release/integration-check.log  | grep -v osgikernel | wc -l `
testsrun=`grep "failures" last-release/integration-check.log  | wc -l`
if [[ $testsrun -eq 0 ]]
then
   echo "No tests were run, cant perform release"
   cat last-release/integration-check.log 
   exit -1
fi
echo "$testsrun tests completed"

if [ $errors -ne 0 -o $failures -ne 0 ]
then
   echo "There were failures or errors in integration, cant perform release"
   set +o errexit
   grep -v "0 errors" last-release/integration-check.log  | grep -v osgikernel
   grep -v "0 failures" last-release/integration-check.log  | grep -v osgikernel
   exit -1
fi
    
echo "All Ok, release is good,  Comitting, tagging and moving on"



git add last-release
git commit -a -m "[release-script] preparing for release tag"
git tag -s -m "[release-script] tagging release $cversion " $cversion HEAD
patch -p3 -R < last-release/changeversion.diff

listofpoms=`find . -name pom.xml | grep -v target`
listofpomswithversion=`grep -l $cversion-SNAPSHOT $listofpoms`
for i in $listofpomswithversion
do
  sed "s/$cversion-SNAPSHOT/$nversion-SNAPSHOT/" $i > $i.new
  mv $i.new $i
done
date > last-release/stage3
git add last-release
git commit -a -m "[release-script] new development version"




