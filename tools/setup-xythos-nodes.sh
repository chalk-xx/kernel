#!/bin/sh
curl -F"sling:resourceType=sakai/external-repository" -F"jcr:mixinTypes=mix:referenceable" -F"sakai:repository-processor=xythos" http://admin:admin@alexqa.home.nyu.edu:8080/xythos
UUID=`curl http://alexqa.home.nyu.edu:8080/xythos.json | sed 's/.*uuid"\:"\(.*\)".*/\1/g'`
curl -F"sling:resourceType=sakai/external-repository-search" -F"sakai:repository-ref=$UUID" -F"sakai:search-prop-q={search|*}" http://admin:admin@alexqa.home.nyu.edu:8080/var/search/xythos