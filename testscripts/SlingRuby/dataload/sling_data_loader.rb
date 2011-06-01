#!/usr/bin/env ruby
require 'rubygems'
require 'optparse'
require 'json'
require 'digest/sha1'
require 'net/http/post/multipart'
require '../kerns/ruby-lib-dir.rb'
require 'sling/sling'
require 'sling/users'
require 'sling/file'
require 'full_group_creator'
include SlingInterface
include SlingUsers
include SlingFile

module NakamuraData
  
  # this script MUST be run from working directory
  # 
  class SlingDataLoader
  
    TEST_GROUP_PREFIX = 'group'
    FIRST_NAMES_FILE = '../../jmeter/firstnames.csv'
    LAST_NAMES_FILE = '../../jmeter/lastnames.csv'
    CATEGORIES_FILE = "categories.txt"
    TAGS_FILE = "tags.txt"

    @full_group_creator = nil
    @sling = nil
    @file_manager = nil

    attr_reader :log, :file_log, :task, :category_index, :tag_index
    
    def initialize(options)
      @upload_success_count = 0
      @upload_failure_count = 0
      @category_index = 0
      @tag_index = 0
      @user_ids_file = options[:usersfile]
      @num_groups = options[:numgroups].to_i
      @groups_per_user = options[:groupsperuser].to_i
      @load_content_files = options[:loadfiles].to_i
      @content_root = options[:contentroot]
      @task = options[:task]
      
      @groups = []
      @user_ids = []
      server_url = options[:appserver]
      admin_password = options[:adminpwd]
      @sling = Sling.new(server_url, false)
      @sling.log.level = Logger::INFO
      @sling.do_login
      @log = Logger.new(STDOUT)
      @log.level = Logger::INFO
      @file_log = Logger.new('load.log', 'daily')
      @file_log.level = Logger::INFO
      @full_group_creator = SlingUsers::FullGroupCreator.new @sling, @file_log
      @full_group_creator.log.level = Logger::INFO
      @file_manager = FileManager.new(@sling)
    end
    
    def load_users_data
      user_ids_file = File.open(@user_ids_file, "r")
      @user_ids = user_ids_file.readlines
      user_ids_file.close
      
      first_names_file = File.open(FIRST_NAMES_FILE, "r")
      @first_names = first_names_file.readlines
      first_names_file.close
      
      last_names_file = File.open(LAST_NAMES_FILE, "r")
      @last_names = last_names_file.readlines
      last_names_file.close
    end
    
    def load_categories
      categories_file = File.open(CATEGORIES_FILE, "r")
      @categories = categories_file.readlines('\n').join.split(',')
      @categories.each do |category|
	category.chomp!
	category.lstrip!
      end
      if ("".eql? @categories[@categories.length - 1])
	@categories.delete_at(@categories.length - 1) 
      end
      @log.info "categories are #{@categories.inspect}"
      @file_log.info "categories are #{@categories.inspect}"
    end
    
    def load_tags
      tags_file = File.open(TAGS_FILE, "r")
      @tags = tags_file.readlines('\n').join.split(',')
      @tags.each do |tag|
	tag.chomp!
	tag.lstrip!
      end
      if ("".eql? @tags[@tags.length - 1])
	@tags.delete_at(@tags.length - 1) 
      end
      @log.info "tags are #{@tags.inspect}"
      @file_log.info "tags are #{@tags.inspect}"      
    end
    
    # create users from the generated user_ids file
    def create_users()
      if(@user_ids && @first_names && @last_names)
	@log.info "loading #{@user_ids.length} users"
	@user_ids.each do |user_id|
	  # generated user_ids include password, so strip the unused password
	  user_id = user_id.split(",")[0]
	  first_name = @first_names[rand(@first_names.length)]
	  last_name = @last_names[rand(@last_names.length)]
	  target_user = @full_group_creator.create_user user_id.chomp, first_name.chomp, last_name.chomp
	  if(target_user)
	    @log.info("created user: #{target_user.inspect}")
	    @file_log.info("created user: #{target_user.inspect}")
	  else
	    @log.info("user #{user_id} not created, may already exist?")
	    @file_log.info("user #{user_id} not created, may already exist?")	    
	    target_user = User.new user_id
	  end
	end
      else
	put 'failure loading user_ids, first_names or last_names'
      end
    end
    
    #follow the UI's sequence of posts to create fully functional groups/worlds
    # including the intial sakai docs for Library and Participants
    def create_full_groups
      count = 0
      while count < @num_groups
	group_name = "#{TEST_GROUP_PREFIX}-#{count.to_s}"
	creator_id = @user_ids[rand(@user_ids.length)]
	creator_id = creator_id.split(",")[0]
	group = @full_group_creator.create_full_group creator_id, group_name, "#{group_name} Title", "#{group_name} Description"
	count = count + 1
	@groups << group
      end
    end
    
    # create the requested groups and add one manager to each group
    # this now deprecated in favor of create_full_groups although
    # it could still be used with a command line switch
    def create_groups
      count = 0
      while count < @num_groups
	group_name = "#{TEST_GROUP_PREFIX}-#{count.to_s}"
	#def create_group_complete(groupname, manager, title = nil)
	group = @full_group_creator.create_group group_name, "Test Group #{count.to_s}"
	if(group)
	  @log.info "created group: #{group.name}"
	  @file_log.info "created group: #{group.name}"	  
	else
	  @log.info("group #{group_name} not created, may already exist?")
	  @file_log.info("group #{group_name} not created, may already exist?")	  
	  group = Group.new group_name
	end
	@groups << group
	count = count + 1
	# add manager after group creation, doesn't appear to work on same post
	manager_id = @user_ids[rand(@user_ids.length)]
	manager_id = manager_id.split(",")[0]
	result = add_group_manager group_name, manager_id
	@log.info "result: #{result.inspect} from adding manager: #{manager_id} to group: #{group.name}"
	if (result.code.to_i > 299)
	    @log.warn "error adding manager: #{manager_id} to group: #{group.name}"
	    @file_log.warn "error adding manager: #{manager_id} to group: #{group.name}"	    
	  else
	    @log.info "added manager: #{manager_id} to group: #{group.name}"
	    @file_log.info "added manager: #{manager_id} to group: #{group.name}"
	  end
      end
    end
    
    # because group.add_manager doesn't to the right thing, adding this method here
    def add_group_manager group_name, principal
      uri = $USERMANAGER_URI + "group/#{group_name}-managers.update.json"
      result = @sling.execute_post(@sling.url_for(uri), { ":member" => principal })
    end
    
    # put each user into however many groups are specified on command line    
    def join_groups
      @user_ids.each do |user_id|
      user_id = user_id.split(",")[0]
        group_count = 0
	while group_count < @groups_per_user
	  group = @groups[rand(@groups.length)]
	  result = group.add_member @sling, user_id, "user"
	  if (result.code.to_i > 299)
	    @log.warn "error user: #{user_id} to group: #{group.name}"
	    @file_log.warn "error user: #{user_id} to group: #{group.name}"	    
	  else
	    @log.info "added user: #{user_id} to group: #{group.name}"
	    @file_log.info "added user: #{user_id} to group: #{group.name}"	    
	  end
	  group_count = group_count + 1
	end
      end
    end
    
    # load some Lorem Ipsum generated text files and, if specified, load the NYU Content
    def load_content
      load_categories
      load_tags
      #load_simple_content -- per Alan no need for lorem ipsum content
      if(@load_content_files == 1)
	load_files_from_filesystem @content_root
      end
	@log.info("Total files uploaded: #{@upload_success_count.to_s}")
	@log.info("File upload failures: #{@upload_failure_count.to_s}")
	@file_log.info("Total files uploaded: #{@upload_success_count.to_s}")
	@file_log.info("File upload failures: #{@upload_failure_count.to_s}")         
    end
    
        
    # get lorem content to upload, just uploading plain text
    def load_simple_content
      copies = 1..10
      copies.each do |num|
	begin
	# get lorem content to upload
	  req = Net::HTTP::Get.new('/feed/json?amount=1&generate=Generate%20Lorem%20Ipsum&start=yes&what=paras')
	  lorem_response = Net::HTTP.new('www.lipsum.com').start { |http| http.request(req) }
	  lorem_json = JSON.parse lorem_response.body
	  lorem_text = lorem_json['feed']['lipsum']
	  @log.debug lorem_text
	  file_name = "Lorem_Ipsum_#{rand(1000)}"
	  res = @file_manager.upload_pooled_file(file_name, lorem_text, 'text/plain')
	  if (res.code.to_i < 299)  
	    @log.info("uploaded lorem content: #{file_name}")
	    @file_log.info("uploaded lorem content: #{file_name}")
	    @upload_success_count = @upload_success_count + 1
	  else
	    @log.info("failed uploading lorem content: #{file_name}")
	    @file_log.info("failed uploading lorem content: #{file_name}")	    
	    @upload_failure_count = @upload_failure_count + 1
	  end
	  file_extension = ".txt"
	  json = JSON.parse(res.body)
	  contentid = json[file_name]['poolId']
	  # in addition to the upload, the following properties need to be set for fully functional, viewable content
	  finish_content contentid, file_name, file_extension
	  categorize_content contentid
	  tag_content contentid	  
	rescue Exception => ex
	  @log.warn "failed loading simple content file: #{file_name}"
	end
      end
    end
    
    # load the NYU content if requested
    def load_files_from_filesystem(rootdir_name)
      ignore_dirs = ['.','..']
      begin
	Dir.foreach(rootdir_name) do |dir_name|
	  @log.debug "Got #{dir_name}"
	  if (!ignore_dirs.include? dir_name)
	    # this is a top level content containing directory e.g. doc
	    content_dir = Dir.new rootdir_name + '/' + dir_name
	    content_dir.each do |content_file_name|
	      if (!ignore_dirs.include? content_file_name)
		@log.debug "Got content file name: #{content_file_name}"
		# we're not going to do the recursive thing, so just bail if we hit a subdirectory
		begin
		  load_file_from_filesystem content_dir.path, content_file_name, get_mime_type(content_file_name)
		rescue Exception => ex
		  @log.warn "Failed uploading #{content_file_name} because #{ex.class}: #{ex.message}"
		  @log.warn("failed uploading file: #{content_file_name} 0" )
		  @file_log.warn "Failed uploading #{content_file_name} because #{ex.class}: #{ex.message}"
		  @file_log.warn("failed uploading file: #{content_file_name} 0" )		
		  @upload_failure_count = @upload_failure_count + 1
		end	
	      end
	    end
	  end
	end
      rescue Exception => ex
	@log.warn "failed to load content from root dir #{rootdir_name}"
	@file_log.warn "failed to load content from root dir #{rootdir_name}"
      end 	
    end
    
    
    def load_file_from_filesystem(directory_name, full_file_name, mime_type)
      url = URI.parse(@sling.url_for("/system/pool/createfile"))
      #return @sling.execute_file_post(@sling.url_for("/system/pool/createfile"), name, name, data, content_type)mj
      #because the content file name doesn't have suffix in nakarmura for some reason
      last_dot = full_file_name.rindex('.')
      file_name = full_file_name.slice(0, last_dot)
      file_extension = full_file_name.slice(last_dot, (file_name.length - 1))
      @log.info "uploading #{full_file_name} of mime type: #{mime_type}"
      @file_log.info "uploading #{full_file_name} of mime type: #{mime_type}"      
      File.open("#{directory_name}/#{full_file_name}") do |file|
	req = Net::HTTP::Post::Multipart.new url.path,
	  "file" => UploadIO.new(file, mime_type, file_name)
	@sling.set_cookies(req)
	res = Net::HTTP.start(url.host, url.port) do |http|
	  http.request(req)
	end
	if (res.code.to_i < 299)
	  json = JSON.parse(res.body)
	  contentid = json[file_name]["poolId"]	
	  @log.info("uploaded file: #{file_name} with content_id: #{contentid} 1" )
	  @file_log.info("uploaded file: #{file_name} with content_id: #{contentid} 1" )
	  @upload_success_count = @upload_success_count + 1
	  finish_content contentid, file_name, file_extension
	  categorize_content contentid
	  tag_content contentid
	else
	  @log.info("failed to upload file: #{file_name} 0" )
	  @file_log.info("failed to upload file: #{file_name} 0" )
	  @upload_failure_count = @upload_failure_count + 1	  
	end
      end
    end
    
    def get_mime_type(file_name)
      basename = File.basename(file_name).downcase
      mime_type = ""
      if (basename.include? 'docx')
	mime_type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      elsif (basename.include? 'doc')
	mime_type = "application/msword"
      elsif (basename.include?('html') || basename.include?('htm'))
	mime_type = "text/html"
      elsif (basename.include? 'gif')
	mime_type = "image/gif"
      elsif (basename.include? 'jpg' || basename.include?('jpeg'))
	mime_type = "image/jpeg"
      elsif (basename.include? 'pdf')
	mime_type = "application/pdf"	
      elsif (basename.include? 'xlsx')
	mime_type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"	
      elsif (basename.include? 'xls')
	mime_type = "application/vnd.ms-excel"
      else
	mime_type = "application/octet-stream"
      end
      return mime_type
    end
    
    # do the same thing the UI will do when uploading content
    def finish_content(contentid, file_name, file_extension)
      res = @sling.execute_post(@sling.url_for("/p/#{contentid}"), {
	"sakai:fileextension" => file_extension,
	"sakai:pooled-content-file-name" => file_name + file_extension
      })
      res = @sling.execute_post(@sling.url_for("/p/#{contentid}"), {
	"sakai:permissions" => "everyone"
      })
      res = @sling.execute_post(@sling.url_for("/p/#{contentid}.modifyAce.html"), {
	"principalId" => "everyone",
	"privilege@jcr:read" => "granted"
      })
    end

    def categorize_content(contentid)
      category = get_category
      @log.info("creating category tag for category #{category}")
      @file_log.info("creating category tag for category #{category}")    
      batch_post = []
      batch_post[0] = {"url" => "/tags/directory/#{category}", "method" => "POST", "parameters" => {"sakai:tag-name" => "directory/#{category}", "sling:resourceType" => "sakai/tag", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post_json = JSON.generate batch_post
      parameters = {"requests" => batch_post_json}
      @log.debug("creating category tag batch post is: #{batch_post_json}")
      @file_log.debug("creating category tag batch post is: #{batch_post_json}")   
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("creating category response code is: #{response.code}")
      @file_log.info("creating category response code is: #{response.code}")
      
      @log.info("applying category tag #{category} to content item #{contentid}")
      @file_log.info("applying category tag #{category} to content item #{contentid}")   
      batch_post = []
      batch_post[0] = {"url" => "/p/#{contentid}", "method" => "POST", "parameters" => {"key" => "/tags/directory/#{category}", ":operation" => "tag","_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post_json = JSON.generate batch_post
      @log.debug("applying category tag batch post is: #{batch_post_json}")
      @file_log.debug("applying category tag batch post is: #{batch_post_json}")  
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("creating category response code is: #{response.code}")
      @file_log.info("creating category response code is: #{response.code}")
    end
    
#    	[{"url":"/tags/tag1","method":"POST","parameters":{"sakai:tag-name":"tag1","sling:resourceType":"sakai/tag","_charset_":"utf-8"},"_charset_":"utf-8"},\
#	{"url":"/tags/tag2","method":"POST","parameters":{"sakai:tag-name":"tag2","sling:resourceType":"sakai/tag","_charset_":"utf-8"},"_charset_":"utf-8"}]

#	[{"url":"/p/h5Voiiymib","method":"POST","parameters":{"key":"/tags/tag1",":operation":"tag","_charset_":"utf-8"},"_charset_":"utf-8"},\
#	{"url":"/p/h5Voiiymib","method":"POST","parameters":{"key":"/tags/tag2",":operation":"tag","_charset_":"utf-8"},"_charset_":"utf-8"}]
    def tag_content contentid
      tag_name1 = get_tag
      tag_name2 = get_tag
      @log.info("creating tags #{tag_name1} and #{tag_name2}")
      @file_log.info("creating tags #{tag_name1} and #{tag_name2}")
      
      batch_post = []
      batch_post[0] = {"url" => "/tags/#{tag_name1}", "method" => "POST", "parameters" => {"sakai:tag-name" => "#{tag_name1}", "sling:resourceType" => "sakai/tag", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post[1] = {"url" => "/tags/#{tag_name2}", "method" => "POST", "parameters" => {"sakai:tag-name" => "#{tag_name2}", "sling:resourceType" => "sakai/tag", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post_json = JSON.generate batch_post
      @log.debug("creating tags batch post is: #{batch_post_json}")
      @file_log.debug("applying tags batch post is: #{batch_post_json}")      
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("creating tags response code is: #{response.code}")
      @file_log.info("creating tags response code is: #{response.code}")
      
      @log.info("applying tags #{tag_name1} and #{tag_name2} to content item #{contentid}")
      @file_log.info("applying tags #{tag_name1} and #{tag_name2} to content item #{contentid}")
      batch_post = []
      batch_post[0] = {"url" => "/p/#{contentid}", "method" => "POST", "parameters" => {"key" => "/tags/#{tag_name1}", ":operation" => "tag","_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post[1] = {"url" => "/p/#{contentid}", "method" => "POST", "parameters" => {"key" => "/tags/#{tag_name2}", ":operation" => "tag","_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post_json = JSON.generate batch_post
      @log.debug("applying category tag batch post is: #{batch_post_json}")
      @file_log.debug("applying category tag batch post is: #{batch_post_json}")  
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("applying tags response code is: #{response.code}")
      @file_log.info("applying tags response code is: #{response.code}")
    end
    
    
     
    def get_category
      @category_index = 0 if (@category_index == @categories.length)
      category = @categories[@category_index]
      @category_index = @category_index + 1
      return category
    end
    
    def get_tag
      @tag_index = 0 if (@tag_index == @tags.length)
      tag = @tags[@tag_index]
      @tag_index = @tag_index + 1
      return tag      
    end
  end
if ($PROGRAM_NAME.include? 'sling_data_loader.rb')
  options = {}
  optparser = OptionParser.new do |opts|
    opts.banner = "Usage: sling_data_loader.rb [options]"

    # trailing slash is mandatory
    options[:appserver] = "http://localhost:8080/" 
    opts.on("-s", "--server [APPSERVE]", "Application Server") do |as|
      options[:appserver] = as
    end
    
    options[:adminpwd] = "admin"
    opts.on("-a", "--adminpwd [ADMINPWD]", "Application Admin User Password") do |ap|
      options[:adminpwd] = ap
    end
    
    opts.on("-u", "--userids USERIDS", "File of user_ids") do |nu|
      options[:usersfile] = nu
    end
    
    options[:numgroups] = 200
    opts.on("-g", "--num-groups [NUMGROUPS]", "Number of groups to create, default is 200") do |ng|
      options[:numgroups] = ng
    end
    
    options[:groupsperuser] = 2
    opts.on("-m", "--groups-per-user [GROUPSPERUSER]", "Number of groups that user is a member of, default is 2") do |oi|
      options[:groupsperuser] = oi
    end
    
    options[:loadfiles] = 1
    opts.on("-f", "--load-content-files [CONTENTFILES]", "Load static content files, default is 1") do |lf|
      options[:loadfiles] = lf
    end
    
    options[:contentroot] = './TestContent'
    opts.on("-r", "--content-root [CONTENTROOT]", "Root Directory of Content files, default is './TestContent'") do |cr|
      options[:contentroot] = cr
    end
    
    options[:task] = 'all'
    # tasks are 'all' (the default), 'usersandgroups' or 'content'
    opts.on("-t", "--task [TASK]", "The task or tasks to perform one of 'all'(the default), 'usersandgroups', 'content'" ) do |tk|
      options[:task] = tk
    end    
  end
end 
  optparser.parse ARGV
  
  sdl = NakamuraData::SlingDataLoader.new options
  sdl.load_users_data
  if (sdl.task == 'all')
    sdl.create_users
    sdl.create_full_groups
    sdl.join_groups
    sdl.load_content
  elsif (sdl.task == 'usersandgroups')
    sdl.create_users
    sdl.create_full_groups
    sdl.join_groups
  elsif (sdl.task == 'content')
    sdl.load_content    
  else
    sdl.log.warn("-t --task parameter incorrect, @task is #{sdl.task}")
  end

end