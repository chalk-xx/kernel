#!/bin/bash
# A simple script for generating a set of users, groups, sites, and memberships for testing for testing presence
# forcing a login in web browser
# http://localhost:8080/?sling:authRequestLogin=1
# create some users
curl -u admin:admin -F:name=aaron -Fpwd=aaron -FpwdConfirm=aaron http://localhost:8080/system/userManager/user.create.html
curl -u admin:admin -F:name=becky -Fpwd=becky -FpwdConfirm=becky http://localhost:8080/system/userManager/user.create.html
curl -u admin:admin -F:name=kitty -Fpwd=kitty -FpwdConfirm=kitty http://localhost:8080/system/userManager/user.create.html
# GET http://localhost:8080/system/userManager/user/aaron.json
# create some groups
curl -u admin:admin -F:name=g-group1 http://localhost:8080/system/userManager/group.create.html
# GET http://localhost:8080/system/userManager/group/g-group1.json to check it exists
# put the users in some groups
# this should be changed later on so that the /system/userManager/user/<username> prefix is not needed and <username> can be used alone
curl -u admin:admin -F:member=/system/userManager/user/aaron -F:member=/system/userManager/user/becky http://localhost:8080/system/userManager/group/g-group1.update.html
# GET http://localhost:8080/system/userManager/group/g-group1.json to check if members are in the group (should see two)
# make the users connect with each other
curl -u aaron:aaron -F"types=spouse" -X POST http://localhost:8080/_user/contacts/becky.invite.html
curl -u aaron:aaron -F"types=pet" -X POST http://localhost:8080/_user/contacts/kitty.invite.html
curl -u becky:becky -X POST http://localhost:8080/_user/contacts/aaron.accept.html
# kitty does not connect though
# try to get the status of the current user
curl -u aaron:aaron http://localhost:8080/_user/presence.json
curl -u becky:becky http://localhost:8080/_user/presence.json
# check getting the status of my contacts
curl -u aaron:aaron http://localhost:8080/_user/presence.contacts.json
# try to change my status
curl -u aaron:aaron -X PUT http://localhost:8080/_user/presence.json?sakai:status=Testing
# get the new status to check it
curl -u aaron:aaron http://localhost:8080/_user/presence.json
# try to ping with my location
curl -u aaron:aaron -X POST http://localhost:8080/_user/presence.json?sakai:location=Testing
# get the new status to check it
curl -u aaron:aaron http://localhost:8080/_user/presence.json
# try to set both at once
curl -u aaron:aaron -X PUT http://localhost:8080/_user/presence.json?sakai:status=Testing2&sakai:location=Testing2
# get the new status to check it
curl -u aaron:aaron http://localhost:8080/_user/presence.json
# try to clear my status
curl -u aaron:aaron -X DELETE http://localhost:8080/_user/presence.json
# get the new status to check it is cleared
curl -u aaron:aaron http://localhost:8080/_user/presence.json
