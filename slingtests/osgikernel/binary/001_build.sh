#!/bin/bash

# ------------------------------------------------------
#
# Build script, to generate *NIX tarballs, and Windows
# friendly zip files.
#
# ------------------------------------------------------



# ---------------------------
# User customisable ENV VARs
# ---------------------------

K2_version="sakai-k2-0.1-SNAPSHOT-binary"
mvn_bin="/usr/bin/mvn"
tar_bin="/bin/tar"
zip_bin="/usr/bin/zip"


#-------------------------------
#   DO NOT EDIT BELOW THIS LINE
# -------------------------------

export K2_version
export mvn_bin
export tar_bin
export zip_bin

# -----------------------------------
# Define ENV VARs used in the script
# -----------------------------------


export target=release
export build_root=`pwd`
export MAVEN_OPTS=-Xmx1024M

# -----------------------
# Check for requirements
# -----------------------




# ------------------------
# Create folders required 
# ------------------------
clear
echo "Creating requisite folders for the build folder"
mkdir ${target} > /dev/null
mkdir ${target}/${K2_version} > /dev/null 
mkdir ${target}/${K2_version}/lib > /dev/null
mkdir ${target}/${K2_version}/src > /dev/null 

#pushd ${build_root}/../
#${mvn_bin} clean install
#popd


cp ${build_root}/../app/target/*SNAPSHOT.jar ${build_root}/${target}/${K2_version}/lib
cp ${build_root}/../app/target/*sources.jar ${build_root}/${target}/${K2_version}/src
cp -r ${build_root}/src/main/configuration/ ${build_root}/${target}/${K2_version}/
cp ${build_root}/../app/LICENSE ${build_root}/${target}/${K2_version}/
cp ${build_root}/../app/NOTICE ${build_root}/${target}/${K2_version}/


# Generate README.txt in the root of ${build_root}/${target}/${K2_version}

${build_root}/010_gen_README.sh  

pushd ${build_root}/${target}/

${tar_bin} cvzf ${K2_version}.tar.gz ${K2_version} 
${zip_bin} -r ${K2_version}.zip ${K2_version} 

popd



