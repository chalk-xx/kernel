#!/bin/sh
for i in `find . -type f`
do 
  cat $i | openssl md5 > $i.md5
  cat $i | openssl sha > $i.sha1
done
for i in `find . -type f`
do 
   gpg -a -b $* $i
done

