This is the SiteService bundle that manages Sakai Site nodes.

Sakai Site Nodes are normal content nodes, addressable by any URL, with the resourceType sakai/site.

To create or convert an existing node into a site node perform a post setting the resourceType 
to sakai/site.

eg as admin

 curl -F"sakai:title=Site Title" -F"sling:resourceType=sakai/site"  \
      -u admin  http://localhost:8080/sites/testsite5

Sites may have a number of properties, set by setting POST properties in the 
same way as any other Sling POST.

The Sites are rendered by navigating to the site URL eg 
 http://localhost:8080/sites/testsite5.html
 
 which will render an HTML container page based on the 
 sakai:site-template
property which points to a location in the JCR.

By default this points to /sites/default.html 
 
Some sample URLs for testing:
# create some users
curl -u admin:admin -F:name=aaron -Fpwd=aaron -FpwdConfirm=aaron http://localhost:8080/system/userManager/user.create.html
curl -u admin:admin -F:name=nico -Fpwd=nico -FpwdConfirm=nico http://localhost:8080/system/userManager/user.create.html
curl -u admin:admin -F:name=ian -Fpwd=ian -FpwdConfirm=ian http://localhost:8080/system/userManager/user.create.html
# GET http://localhost:8080/system/userManager/user/aaron.json
# create some groups
curl -u admin:admin -F:name=g-group1 http://localhost:8080/system/userManager/group.create.html
curl -u admin:admin -F:name=g-group2 http://localhost:8080/system/userManager/group.create.html
curl -u admin:admin -F:name=g-group3 http://localhost:8080/system/userManager/group.create.html
# GET http://localhost:8080/system/userManager/group/g-group1.json to check it exists
# put the users in some groups
# this should be changed later on so that the /system/userManager/user/<username> prefix is not needed and <username> can be used alone
curl -u admin:admin -F:member=/system/userManager/user/aaron -F:member=/system/userManager/user/nico http://localhost:8080/system/userManager/group/g-group1.update.html
curl -u admin:admin -F:member=/system/userManager/user/aaron -F:member=/system/userManager/user/ian http://localhost:8080/system/userManager/group/g-group2.update.html
curl -u admin:admin -F:member=/system/userManager/user/nico http://localhost:8080/system/userManager/group/g-group3.update.html
# GET http://localhost:8080/system/userManager/group/g-group1.json to check if members are in the group (should see two)
# create site
curl -u admin:admin -F"sakai:title=My Site1" -F"sling:resourceType=sakai/site" -F"sakai:authorizables=g-group1" -F"sakai:authorizables=g-group2" http://localhost:8080/sites/site1
curl -u admin:admin -F"sakai:title=My Site2" -F"sling:resourceType=sakai/site" -F"sakai:authorizables=g-group2" http://localhost:8080/sites/site2
curl -u admin:admin -F"sakai:title=My Site3" -F"sling:resourceType=sakai/site" -F"sakai:authorizables=g-group3" http://localhost:8080/sites/site3
# add a few members
#curl -u admin:admin -F"targetGroup=group1" http://localhost:8080/sites/site1.join.html
# this is not right
#curl -u admin:admin -F"sakai:authorizables=aaron" http://localhost:8080/sites/site1
