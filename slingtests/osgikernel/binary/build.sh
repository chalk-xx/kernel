#!/bin/sh
mkdir target
pushd target
version=sakai-k2-0.1-SNAPSHOT-binary
mkdir ${version}
mkdir ${version}/lib
mkdir ${version}/src
cp ../../app/target/*SNAPSHOT.jar ${version}/lib
cp ../../app/target/*sources.jar ${version}/src
cp -r ../src/main/configuration/ ${version}/
cp ../../LICENSE ${version}/
cp ../../NOTICE ${version}/
tar cvzf ${version}.tar.gz ${version}
zip -r ${version}.zip ${version}

