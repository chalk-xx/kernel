#!/usr/bin/env ruby

module SlingFile
  
  $testfile1 = "<html><head><title>KERN 312</title></head><body><p>Should work</p></body></html>"
  
  class FileManager
    
    def initialize(sling)
      @sling = sling
    end
    
    def upload(link, site, props = {})
      return @sling.execute_file_post("http://localhost:8080/_user/files.upload.html", "Filedata", "myFile.html", $testfile1, "text/html")
    end
    
    def download(fileId)
      return @sling.execute_get(@sling.url_for("/_user/files/#{fileId}"))
    end
    
    def createlink(fileId, link, site, props = {})
      return @sling.execute_post(@sling.url_for("/_user/files/#{fileId}.link.json"), props.update("link" => link, "site" => site))
    end
    
    def myfiles(search)
      return @sling.execute_get(@sling.url_for("/var/search/files/myfiles.json?search=#{search}"))
    end
    
    
  end
  
end
