README for running a sling data loading script - sling_data_loader.rb

see KERN-1730

all scripts have to be run from this directory as working directory

Dependencies and Requirements
1) ruby installed with the gems described in 
nakamura/testscripts/SlingRuby/README.txt

2) Additional ruby gem for file uploads
sudo gem install multipart-post

3) uses script to generate user_ids
nakamura/testscripts/jmeter/netidusers.pl

4) uses data files for first and last names
nakamura/testscripts/jmeter/firstnames.csv
nakamura/testscripts/jmeter/lastnames.csv

5) uses content retrieved from NYU and unzipped in working directory

Procedure:
1) generate new user ids file - e.g. someusers.csv - see comment in netidusers.pl
../../jmeter/netidusers.pl 6100 | sort | uniq | head -n 6000 > someusers.csv

generates 6000 unique user_ids.  Comment in script says this is max number but I have generated
20000 users successfully.  Note that it also generates a passowrd of "test" for each user_id
but the sling_data_loader does not use this password.  It uses the default password in
SlingRuby integration tests."

2) retrieve NYU content that is not checked in in Git and unzip it in working directory
curl https://files.nyu.edu/maw1/public/kern-1306/TestContent.zip > TestContent.zip
unzip TestContent.zip

3) typical command line evocations of sling_data_loader
ruby sling_data_loader.rb --server http://localhost:8080/ --adminpwd admin --userids someusers.csv --num-groups 200 --groups-per-user 2 --load-content-files 1
or
ruby sling_data_loader.rb -s http://localhost:8080/ -a admin -u someusers.csv -g 200 -m 2 -f 1

Command notes:
1) admin user does all the data loading
2) the trailing slash on --server value is required
3) --load-content-files refers to the 600 MBytes of NYU content, simple text file content "Lorem Ipsum" files will be generated in any case
   --load-content-files has 2 values, 0 for do not load or 1 for do load the files.
4) after loading users, you can login as any user by picking one of the id's someusers.csv with the default password

Timing Notes: Using my MacbookPro as both localhost server and ruby client,
I was able to load 500 users in 3 minutes.  That means 10,000 users would
take 1 hour on localhost and longer remotely.

I loaded (almost) all of the NYU content in 10 minutes