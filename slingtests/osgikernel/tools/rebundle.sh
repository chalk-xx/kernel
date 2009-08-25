#!/bin/sh

for i in $*
do
  pushd bundles/$i
  mvn clean install
  if [[ $? -ne 0 ]]
  then
     echo "Build of $i Failed "
     exit 10
  fi
  popd
done
mvn -Pbundle clean install
  if [[ $? -ne 0 ]]
  then
     echo "Bundle Failed "
     exit 10
  fi

