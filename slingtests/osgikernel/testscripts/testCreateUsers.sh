#!/bin/sh
n=`date "+%Y%m%d%H%M%S"`
x=1
while [[ $x -lt 25000 ]]
do
  curl -F:name=testuser${n}-${x} -Fpwd=testuser -FpwdConfirm=testuser http://localhost:8080/system/userManager/user.create.html
  echo Created testuser${n}-${x}
  let x=x+1
done

