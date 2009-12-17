Ruby REST API's

This is a REST Client Library for Sakai K2 written in Ruby.

It should run on any platform with ruby installed, however it may need some extra components.

To get these makn certain you have a Ruby installation with Ruby Gems installed. Once you have this try
running the one of the scripts. eg 

create-user.rb testuser

If you get any errors you may need to add some extra components. to go this use Ruby Gems

n OSX 10.5 I needed to do the following.

sudo gem update 
sudo gem install json 
sudo gem install curb

If you are running OS X 10.6, the following commands work:
sudo gem update --system
sudo gem update
sudo gem install json
sudo env ARCHFLAGS="-arch x86_64" gem install curb


