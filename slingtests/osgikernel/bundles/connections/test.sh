#!/bin/bash
# simple script to test things are working
# forcing a login in web browser
# http://localhost:8080/?sling:authRequestLogin=1
# create a few users
curl -u admin:admin -F:name=aaron -Fpwd=aaron -FpwdConfirm=aaron http://localhost:8080/system/userManager/user.create.html
curl -u admin:admin -F:name=nico -Fpwd=nico -FpwdConfirm=nico http://localhost:8080/system/userManager/user.create.html
curl -u admin:admin -F:name=ian -Fpwd=ian -FpwdConfirm=ian http://localhost:8080/system/userManager/user.create.html
# GET http://localhost:8080/system/userManager/user/aaron.json
# now try to do a request from aaron to nico
curl -u aaron:aaron -F"types=friend" -F"types=coworker" -X POST http://localhost:8080/_user/contacts/nico.invite.html
# now try to fetch the connection nodes
curl -u nico:nico http://localhost:8080/_user/contacts/aaron.json
curl -u aaron:aaron http://localhost:8080/_user/contacts/nico.json
# now try to accept it
curl -u nico:nico -X POST http://localhost:8080/_user/contacts/aaron.accept.html
# now try to fetch the updated connection nodes
curl -u nico:nico  http://localhost:8080/_user/contacts/aaron.json
curl -u aaron:aaron  http://localhost:8080/_user/contacts/nico.json
