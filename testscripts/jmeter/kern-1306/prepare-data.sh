#!/bin/bash
# create a netids file
# create netid pairs file
# create user tags file
# create content uploads file
# add users to nakamura
# load-messages.jmx
# load-files.jmx
# load-tags.jmx
[[ -n "$1" ]] || { echo "Usage: prepare-data.sh <number of users to create> <absolute path to content directory for uploads>"; exit 0 ; }

NUMUSERS=$1
CONTENTDIR=$2
NUMMESSAGES=$(( $NUMUSERS * 100 ))
NUMTAGS=$(( $NUMUSERS * 3 ))

echo creating $NUMUSERS netIDs in netids01.csv
perl ../netidusers.pl $NUMUSERS > netids01.csv

echo creating $NUMMESSAGES message recipients in recipients.csv
ruby generate-recipient-list.rb $NUMMESSAGES > recipients.csv

echo creating $NUMTAGS user profile tags in user-tags.csv
ruby generate-user-tags.rb $NUMTAGS > user-tags.csv

echo creating list of content from $CONTENTDIR in content.txt
find $CONTENTDIR -type f > content.txt

echo creating ID,FILEPATH,MIMETYPE for file uploads in file-uploads.csv
ruby generate-file-upload-list.rb > file-uploads.csv

