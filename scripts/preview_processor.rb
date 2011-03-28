#!/usr/bin/env ruby
require 'fileutils'
require 'rubygems'
require 'docsplit'
require 'ruby-lib-dir.rb'
require 'sling/sling'
include SlingInterface

MAIN_DIR = Dir.getwd
DOCS_DIR = "#{MAIN_DIR}/docs"
PREVS_DIR = "#{MAIN_DIR}/previews"

Dir.mkdir DOCS_DIR unless File.directory? DOCS_DIR
Dir.mkdir PREVS_DIR unless File.directory? PREVS_DIR

server=ARGV[0]
@s = Sling.new(server)
res = @s.execute_get(@s.url_for("var/search/needsprocessing.json"))
raise "Failed to retrieve list to process [#{res.code}]" unless res.code == '200'
process = JSON.parse(res.body)

# creating all the pending temp. files in /docs
Dir.chdir DOCS_DIR
process['results'].each do |f|
  # only copy pdf's and odt files
  FileUtils.touch f['jcr:name'] if ['.pdf','.odt'].include? f['sakai:fileextension']
end

# for all items in pending folder
puts "pending files: #{Dir["*"].to_s}"

Dir["*"].each do |id|
  puts "processing #{id}"
  FileUtils.rm id # clean up crew
      
  # making a local copy of this file.
  meta_file = @s.execute_get(@s.url_for("p/#{id}.json"))
  next unless (meta_file.code == '200') # skip it
  meta = JSON.parse(meta_file.body)     
  extension = meta['sakai:fileextension']
  filename = id + extension  
  content_file = @s.execute_get(@s.url_for("p/#{id}"))
  File.open(filename, 'wb') { |f| f.write(content_file.body) }

  # generating image previews of te document
  Docsplit.extract_images filename, :size => '920x', :format => :jpg
  
  Dir.mkdir PREVS_DIR + "/#{id}" unless File.directory? PREVS_DIR + "/#{id}"
  # moving these files to another directory: "thumbnails/filename/index.ext"
  Dir[id + '_*'].each_with_index do |preview, index|
    FileUtils.mv "#{preview}", "#{PREVS_DIR}/#{id}/" + index.to_s + '.jpg'
  end
  
  Dir.chdir PREVS_DIR + "/#{id}"  
  # uploading each preview
  Dir["*"].each_with_index do |screenname,index|
     nbytes = File.size screenname 
     content = nil
     File.open(screenname, "rb" ) { |f| content = f.read(nbytes) }
     @s.execute_file_post @s.url_for("system/pool/createfile.#{id}.page#{index}"), "thumbnail", "thumbnail", content, "image/jpeg"
     puts "Uploaded Preview image to curl #{@s.url_for("p/#{id}.page#{index}.jpg")}"
  end 
  
  # flagging the file as processed
  @s.execute_post @s.url_for("p/#{id}"), { "sakai:needsprocessing" => "false" }
  
  # cleaning crew
  Dir.chdir MAIN_DIR
  puts "cleaning up #{id}"
  FileUtils.remove_dir PREVS_DIR + "/#{id}"
  FileUtils.rm DOCS_DIR + "/#{filename}"
  
  Dir.chdir DOCS_DIR # otherwise it won't find the next file
end

FileUtils.remove_dir PREVS_DIR
FileUtils.remove_dir DOCS_DIR