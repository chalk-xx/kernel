#!/usr/bin/env ruby

module SlingFile
  
  $testfile1 = "<html><head><title>KERN 312</title></head><body><p>Should work</p></body></html>"
  
  class FileManager
    
    def initialize(sling)
      @sling = sling
    end
    
    def createlink(url, linkUid, siteUuid)
      props = {
        ":operation" => "link",
        "link" => linkUid
      }
      if (siteUuid != nil)
        props.update("site" => siteUuid)
      end
      return @sling.execute_post(@sling.url_for(url), props)
    end
    
    def createTag(tagName, url, props = {})
      props.update("./jcr:primaryType" => "nt:folder")
      props.update("./jcr:mixinTypes" => "sakai:propertiesmix")
      props.update("./sling:resourceType" => "sakai/tag")
      props.update("./sakai:tag-name" => tagName)
      return @sling.execute_post(@sling.url_for(url), props)
    end
    
    def tag(url, tagUuid)
      props = {
        ":operation" => "tag",
        "uuid" => tagUuid
      }
      return @sling.execute_post(@sling.url_for(url), props)
    end
    
    def myfiles(search)
      return @sling.execute_get(@sling.url_for("/var/search/files/myfiles.json?search=#{search}"))
    end
    
    
  end
  
end
