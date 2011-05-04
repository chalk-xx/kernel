#!/usr/bin/env ruby
require 'fileutils'
require 'rubygems'
require 'docsplit'
RMAGICK_BYPASS_VERSION_TEST = true # I had a newer version
require 'RMagick'
require 'ruby-lib-dir.rb'
require 'sling/sling'
include SlingInterface

# override the initialize_http_header method that sling.rb overrides
# in order to properly set the referrer
module Net::HTTPHeader
  def initialize_http_header(initheader)
    @header = {"Referer" => [ARGV[0]]}
    return unless initheader
    initheader.each do |key, value|
      warn "net/http: warning: duplicated HTTP header: #{key}" if key?(key) and $VERBOSE
      @header[key.downcase] = [value.strip]
    end
  end
end

server=ARGV[0]
@s = Sling.new(server)

# to run: ./preview_processor.rb http://localhost:8080/
DEBUG = false


def resize_and_write_file filename, filename_output, max_width, max_height
  pic = Magick::Image.read(filename).first
  img_width, img_height = pic.columns, pic.rows

  if img_width > max_width || img_height > max_height
    pic.resize_to_fill! max_width, max_height
  end

  pic = Magick::Image.new(max_width, max_height).composite(pic, Magick::CenterGravity, Magick::OverCompositeOp)
  pic.write filename_output

  nbytes, content = File.size(filename_output), nil
  File.open(filename_output, "rb") { |f| content = f.read nbytes }
  content
end

def process_as_image? extension
  ['.png', '.jpg', '.gif', '.psd'].include? extension
end

def determine_file_extension_with_mime_type mimetype
  fe = `grep #{mimetype} ../mime.types`.gsub(mimetype, '').strip.strip.split(' ')[0]
  if fe == '' || fe.nil?
    ''
  else
    ".#{fe}"
  end
end

res = @s.execute_get(@s.url_for("var/search/needsprocessing.json"))
raise "Failed to retrieve list to process [#{res.code}]" unless res.code == '200'
process = JSON.parse(res.body)

MAIN_DIR = Dir.getwd
DOCS_DIR = "#{MAIN_DIR}/docs"
PREVS_DIR = "#{MAIN_DIR}/previews"

Dir.mkdir DOCS_DIR unless File.directory? DOCS_DIR
Dir.mkdir PREVS_DIR unless File.directory? PREVS_DIR

# create a temp file in the DOCS_DIR for all the pending files.
Dir.chdir DOCS_DIR
process['results'].each do |f|
  FileUtils.touch f['jcr:name']
end

puts "pending files: #{Dir["*"].join(', ')}" if Dir["*"].size > 0
# for all items in pending folder.
Dir["*"].each do |id|
  FileUtils.rm id # removing the temp file, we don't need it anymore.
  puts "processing #{id}"

  begin
    # some meta checking.
    meta_file = @s.execute_get @s.url_for("p/#{id}.json")
    next unless meta_file.code == '200' # skip.
    meta = JSON.parse meta_file.body

    # making a local copy of the file.
    mimeType = meta['_mimeType']
    extension = determine_file_extension_with_mime_type mimeType
    filename = id + extension
    puts "with filename: #{filename}"

    content_file = @s.execute_get @s.url_for("p/#{id}")
    File.open(filename, 'wb') { |f| f.write content_file.body }

    if process_as_image? extension
      # Images don't need a preview so we make a big and small thumbnail instead.

      page_count = 1
      filename_thumb = 'thumb.jpg'

      ## Big thumbnail
      content = resize_and_write_file filename, filename_thumb, 900, 500

      @s.execute_file_post @s.url_for("system/pool/createfile.#{id}.page1-normal"), "thumbnail", "thumbnail", content, "image/jpeg"
      puts "Uploaded thumb to curl #{@s.url_for("p/#{id}.page1-normal.jpg")}"

      ## Small thumbnail
      content = resize_and_write_file filename, filename_thumb, 180, 225

      @s.execute_file_post @s.url_for("system/pool/createfile.#{id}.page1-small"), "thumbnail", "thumbnail", content, "image/jpeg"
      puts "Uploaded thumb to curl #{@s.url_for("p/#{id}.page1-small.jpg")}"

      ## Cleaning crew.
      FileUtils.rm DOCS_DIR + "/#{filename_thumb}" unless DEBUG
    else
      # Generating image previews of te document.
      Docsplit.extract_images filename, :size => '700x', :format => :jpg

      next if Dir[id + '_*'].size == 0 # Skip documents with a pagecount of 0, to be sure.

      Dir.mkdir PREVS_DIR + "/#{id}" unless File.directory? PREVS_DIR + "/#{id}"

      # Moving these previews to another directory: "PREVS_DIR/filename/index.jpg".
      Dir[id + '_*'].sort.each_with_index do |preview, index|
        FileUtils.mv "#{preview}", "#{PREVS_DIR}/#{id}/#{index}.jpg"
      end

      Dir.chdir PREVS_DIR + "/#{id}"
      page_count = Dir["*"].size
      # Upload each preview and create+upload a thumbnail.
      Dir["*"].sort.each_with_index do |filename_p, index|
        nbytes = File.size filename_p
        content = nil
        File.open(filename_p, "rb") { |f| content = f.read nbytes }

        # 1 based index! (necessity for the docpreviewer 3akai-ux widget).
        # id.pagex-normal.jpg
        @s.execute_file_post @s.url_for("system/pool/createfile.#{id}.page#{index+1}-normal"), "thumbnail", "thumbnail", content, "image/jpeg"
        puts "Uploaded image to curl #{@s.url_for("p/#{id}.page#{index+1}-normal.jpg")}"

        # Creating a thumbnail of the preview.
        # id.pagex-small.jpg
        filename_thumb = File.basename(filename_p, '.*') + '.small.jpg'

        content = resize_and_write_file filename_p, filename_thumb, 180, 225

        @s.execute_file_post @s.url_for("system/pool/createfile.#{id}.page#{index+1}-small"), "thumbnail", "thumbnail", content, "image/jpeg"
        puts "Uploaded image to curl #{@s.url_for("p/#{id}.page#{index+1}-small.jpg")}"
      end

      # Cleaning crew.
      FileUtils.remove_dir PREVS_DIR + "/#{id}" unless DEBUG

    end

    # cleaning crew.
    FileUtils.rm DOCS_DIR + "/#{filename}" unless DEBUG

    # passing on the page_count.
    @s.execute_post @s.url_for("p/#{id}"), {"sakai:pagecount" => page_count}

    Dir.chdir DOCS_DIR # otherwise we won't find the next file.
  rescue Exception => msg
    puts "error generating preview/thumbnail (ID: #{id}): #{msg}"
  ensure
    # flagging the file as processed (for both succeeded and failed processes).
    @s.execute_post @s.url_for("p/#{id}"), {"sakai:needsprocessing" => "false"} unless DEBUG
  end
end

FileUtils.remove_dir PREVS_DIR unless DEBUG
FileUtils.remove_dir DOCS_DIR unless DEBUG
