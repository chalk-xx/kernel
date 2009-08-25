This directory contains some scripts

upload.py uploads diffs to codereview.appspot.com for review
clean_restart.sh rebuilds a list of bundles specified on the command line, 
                 rebuilds the application, cleans the previous start and 
                 restarts the app server.
                 if the list of bundles starts with 'all' it will do a full build.	
rebundle.sh rebundles a list of bundles, rebuilding the app
runalltests.sh will search for all testall.sh scripts and run them.
