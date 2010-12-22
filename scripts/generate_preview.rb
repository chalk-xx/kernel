#!/usr/bin/env ruby
require 'ruby-lib-dir.rb'
require 'sling/sling'
require 'RMagick'
require 'fileutils'
include SlingInterface
include Magick


BASE_DIR = "thumbnails"
WORK_DIR = "work"
Dir.mkdir(BASE_DIR) if (!File.directory?(BASE_DIR))
Dir.mkdir(WORK_DIR) if (!File.directory?(WORK_DIR))
server=ARGV[0]
@s = Sling.new(server)
################ @s.switch_user(user)
res = @s.execute_get(@s.url_for("var/search/needsprocessing.json"))
if ( res.code != "200" )
    raise "Failed to retrieve list to process [#{res.code}]"
end
process = JSON.parse(res.body)

# TODO save items to pending folder as files (one file per item)

FileUtils.cd(BASE_DIR)

process['results'].each do |f|
  FileUtils.touch f['jcr:name']
end


# for all items in pending folder
Dir["*"].each do |id|
    
    cf = "../#{WORK_DIR}/#{id}"
    
    # get content url
    # identify content type from metadata (HTTP GET json)
    meta_file = @s.execute_get(@s.url_for("p/#{id}.json"))
    if (meta_file.code == '200')
      meta = JSON.parse(meta_file.body)
    
      # identify thumbnail processor (ImageMagic convert)
      # download content and create thumbnail in a file (HTTP GET)

      content_file = @s.execute_get(@s.url_for("p/#{id}"))
      File.open("#{cf}_img", 'wb') { |f| f.write(content_file.body) }
    
      begin
        thumbnail = Image.read("#{cf}_img").first
        # using resize_to_fill rather than resize_to_fit to give a maximized preview
        thumbnail.resize_to_fill!(640, 640)
        thumbnail.write("#{cf}.jpg")
    
        # upload thumbnail to server (HTTP POST)
        if ( File.exists?("#{cf}.jpg") ) 
          nbytes = File.size("#{cf}.jpg")
          content = nil
          File.open("#{cf}.jpg", "rb" ) { |f| content = f.read(nbytes) }
          @s.execute_file_post(@s.url_for("system/pool/createfile.#{id}.preview"), "thumbnail", "thumbnail", content, "image/jpeg" )
          puts ("Uploaded Thmbnail to curl #{@s.url_for("p/#{id}.preview.jpg")} ")
          # change flag sakai:needsprocessing to 0 (HTTP POST)
          @s.execute_post(@s.url_for("p/#{id}"), { "sakai:needsprocessing" => "false" } )
        else 
          raise
        end
      rescue
        @s.execute_post(@s.url_for("p/#{id}"), { "sakai:needsprocessing" => "failed: no preview" } )
        File.delete("#{id}")
      else
        # clean up working space
        File.delete("#{id}", "#{cf}_img", "#{cf}.jpg")
      end   
    
    else 
      File.delete("#{id}")
    end
end
