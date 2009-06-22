#!/bin/bash
# simple script to test things are working
# create a few users
curl -F:name=aaron -Fpwd=aaron -FpwdConfirm=aaron http://admin:admin@localhost:8080/system/userManager/user.create.html
curl -F:name=nico -Fpwd=nico -FpwdConfirm=nico http://admin:admin@localhost:8080/system/userManager/user.create.html
curl -F:name=ian -Fpwd=ian -FpwdConfirm=ian http://admin:admin@localhost:8080/system/userManager/user.create.html
# now try to do a request from aaron to nico
curl -T -F"types=friend" -F"types=coworker" http://admin:admin@localhost:8080/_user/contacts/nico.request.html
