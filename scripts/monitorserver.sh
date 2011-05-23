#!/bin/bash

#
# Run the preview_processor.rb script every INTERVAL seconds.
#

# In seconds
INTERVAL=15

### No need to edit below this line. ###
PROG=`basename $0`

function usage(){
   cat << EOF

   usage: $PROG [-h|--help] server adminpassword [interval]
   
   example: $PROG http://localhost:8080/ admin 20

EOF
   
}

# server and password are required
if [[ -z $1 || -z $2 ]]; then
    usage
    exit 1
fi

if [[ $1 == "-h" || $1 == "--help " ]]; then
    usage
    exit 0
fi

# interval is optional
if [[ ! -z $3 ]]; then
    INTERVAL=$3
fi

SCRIPTS_DIR=`dirname $0`
pushd $SCRIPTS_DIR > /dev/null
while [[ 1 -eq 1 ]]
do
   ./preview_processor.rb $1 $2
   sleep $INTERVAL
done
# This will probably never be reached but its a good idea to pop what you push.
popd > /dev/null
