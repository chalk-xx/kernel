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
# create site
curl -u admin:admin -F"sakai:title=My Site" -F"sling:resourceType=sakai/site" http://localhost:8080/sites/site1
# add a few members
# this is not right
#curl -u admin:admin -F"sakai:authorizables=aaron" http://localhost:8080/sites/site1
