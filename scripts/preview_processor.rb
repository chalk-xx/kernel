#!/usr/bin/env ruby
require 'fileutils'
require 'rubygems'
require 'docsplit'
require 'RMagick'
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
  FileUtils.touch f['jcr:name']
end

# for all items in pending folder
puts "pending files: #{Dir["*"].to_s}"

Dir["*"].each do |id|
  begin  
  puts "processing #{id}"
  FileUtils.rm id # clean up crew
  
  # some meta checking.    
  meta_file = @s.execute_get(@s.url_for("p/#{id}.json"))
  next unless meta_file.code == '200' # skip it
  meta = JSON.parse(meta_file.body)     
  extension = meta['sakai:fileextension']  
  next if ['.png', '.jpg', '.gif'].include? extension # skip images  

  # making a local copy of this file.
  filename = id + extension  
  content_file = @s.execute_get(@s.url_for("p/#{id}"))
  File.open(filename, 'wb') { |f| f.write(content_file.body) }

  # generating image previews of te document
  Docsplit.extract_images filename, :size => '700x', :format => :jpg
  
  next if Dir[id + '_*'].size == 0 # skip it to be sure
  
  Dir.mkdir PREVS_DIR + "/#{id}" unless File.directory? PREVS_DIR + "/#{id}"
  # moving these files to another directory: "thumbnails/filename/index.ext"
  Dir[id + '_*'].each_with_index do |preview, index|
    FileUtils.mv "#{preview}", "#{PREVS_DIR}/#{id}/#{index}.jpg"
  end
  
  Dir.chdir PREVS_DIR + "/#{id}"  
    
  pagecount = Dir["*"].size
  # uploading each preview
  Dir["*"].each_with_index do |screenname,index|
     nbytes = File.size screenname 
     content = nil
     File.open(screenname, "rb" ) { |f| content = f.read(nbytes) }
     
     # 1 based index! (necessity for the docpreviewer 3akai-ux widget)
     # id.pagex-normal.jpg     
     @s.execute_file_post @s.url_for("system/pool/createfile.#{id}.page#{index+1}-normal"), "thumbnail", "thumbnail", content, "image/jpeg"
     puts "Uploaded image to curl #{@s.url_for("p/#{id}.page#{index+1}-normal.jpg")}"
     
     # creating a thumbnail of the preview
     # id.pagex-small.jpg 
     maxwidth = 180
     maxheight = 225
     aspectratio = maxwidth.to_f / maxheight.to_f

     pic = Magick::Image.read(screenname).first
     imgwidth = pic.columns
     imgheight = pic.rows
     imgratio = imgwidth.to_f / imgheight.to_f
     imgratio > aspectratio ? scaleratio = maxwidth.to_f / imgwidth : scaleratio = maxheight.to_f / imgheight
     thumb = pic.resize scaleratio

     white_bg = Magick::Image.new maxwidth, maxheight
     pic = white_bg.composite thumb, Magick::CenterGravity, Magick::OverCompositeOp
     
     imgfile = File.basename screenname, '.*' # filename without extension
     small_screenname = imgfile + '.small.jpg'
     pic.write small_screenname
     
     nbytes = File.size small_screenname 
     File.open(small_screenname, "rb" ) { |f| content = f.read(nbytes) }
     @s.execute_file_post @s.url_for("system/pool/createfile.#{id}.page#{index+1}-small"), "thumbnail", "thumbnail", content, "image/jpeg"
     puts "Uploaded image to curl #{@s.url_for("p/#{id}.page#{index+1}-small.jpg")}"    
  end  
  
  # passing on the pagecount
  @s.execute_post @s.url_for("p/#{id}"), { "sakai:pagecount" => pagecount }
  
  # cleaning crew
  Dir.chdir MAIN_DIR
  puts "cleaning up #{id}"
  FileUtils.remove_dir PREVS_DIR + "/#{id}"
  FileUtils.rm DOCS_DIR + "/#{filename}"
  
  Dir.chdir DOCS_DIR # otherwise it won't find the next file
rescue
  puts "error generating preview (ID: #{id})"
ensure
  # flagging the file as processed (both succeeded and failed processes)
  @s.execute_post @s.url_for("p/#{id}"), { "sakai:needsprocessing" => "false" }
end
end

FileUtils.remove_dir PREVS_DIR
FileUtils.remove_dir DOCS_DIR