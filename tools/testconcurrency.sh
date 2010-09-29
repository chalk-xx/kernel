#!/bin/sh
uname -a
java -version
echo $1
let x=1
(
while [ $x -lt $2 ]
do 
  ab -q -c $x -n1000  $1 
  let x=x+1
done
) | awk '
BEGIN { print "Concurrency Level,Time taken for tests,Complete requests,Failed requests,Write errors,Total transferred,HTML transferred,Requests per second,Time per request,Time per request,Transfer rate" }
/Concurrency Level/  { n1=$3 };
/Time taken for tests/ { n2=$5 };
/Complete requests/    { n3=$3 } ; 
/Failed requests/      { n4=$3 } ;
/Write errors/         { n5=$3 };
/Total transferred/    { n6=$3 };
/HTML transferred/     { n7=$3 };
/Requests per second/  { n8=$4 };
/Time per request/     { n9=$4 };
/Time per request/     { n10=$4 };
/Transfer rate/        { n11=$3 };
/Connection Times/ { print n1 "," n2 "," n3 "," n4 "," n5 "," n6 "," n7 "," n8 "," n9 "," n10 "," n11  };
'  

