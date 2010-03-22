#!/bin/sh
n=`date "+%Y%m%d%H%M%S"`
x=1
while [[ $x -lt $1 ]]
do
  ((curl -F:name=testuser${n}-${x} -Fpwd=testuser -FpwdConfirm=testuser http://admin:admin@localhost:8080/system/userManager/user.create.html >/dev/null 2>/dev/null \
  && echo Created testuser${n}-${x}) \
  || echo ERROR creating testuser${n}-${x})
  let x=x+1
done

