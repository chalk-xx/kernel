#!/bin/bash
# simple script to test things are working
# forcing a login in web browser
# http://localhost:8080/?sling:authRequestLogin=1
# create a few users
curl -F:name=aaron -Fpwd=aaron -FpwdConfirm=aaron http://admin:admin@localhost:8080/system/userManager/user.create.html
curl -F:name=nico -Fpwd=nico -FpwdConfirm=nico http://admin:admin@localhost:8080/system/userManager/user.create.html
curl -F:name=ian -Fpwd=ian -FpwdConfirm=ian http://admin:admin@localhost:8080/system/userManager/user.create.html
# now try to do a request from aaron to nico
curl -F"types=friend" -F"types=coworker" -X POST http://aaron:aaron@localhost:8080/_user/contacts/nico.request.html
# now try to fetch the connection nodes
curl http://nico:nico@localhost:8080/_user/contacts/aaron.json
curl http://aaron:aaron@localhost:8080/_user/contacts/nico.json
# now try to accept it
curl -X POST http://nico:nico@localhost:8080/_user/contacts/aaron.accept.html
# now try to fetch the updated connection nodes
curl http://nico:nico@localhost:8080/_user/contacts/aaron.json
curl http://aaron:aaron@localhost:8080/_user/contacts/nico.json
