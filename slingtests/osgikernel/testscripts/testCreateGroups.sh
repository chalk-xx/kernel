#!/bin/sh
n=`date "+%Y%m%d%H%M%S"`
x=1
while [[ $x -lt 1000 ]]
do
  curl -F:name=testgroup${n}-${x} --basic -u admin:admin http://localhost:8080/system/userManager/group.create.html
  echo Created testgroup${n}-${x}
  let x=x+1
done

