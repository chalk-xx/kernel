#!/bin/bash
for i in `git branch -a | grep '/master' | grep -v origin | cut -f1 -d'/'`
do 
  echo "Fetching $i"
  git fetch $i
done

