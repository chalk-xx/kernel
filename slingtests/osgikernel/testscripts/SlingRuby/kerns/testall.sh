#!/bin/sh
TESTS="kern-254.rb kern-259.rb kern-270.rb kern-275.rb kern-277.rb kern-278.rb kern-284.rb kern-288.rb kern-289.rb kern-292.rb kern-307.rb kern-308.rb kern-309.rb kern-310.rb kern-312.rb kern-325.rb"
for i in $TESTS
do
echo $i `./$i | grep failure`
done
