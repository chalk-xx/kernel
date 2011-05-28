#!/usr/bin/env ruby

# work on KERN-1881 to duplicate all 11 to 13 POSTs that go into creating a group

require 'json'
require '../kerns/ruby-lib-dir.rb'
require 'sling/sling'
require 'sling/users'
require 'sling/file'
include SlingInterface
include SlingUsers
include SlingFile

$FULL_GROUP_URI="#{$USERMANAGER_URI}group.create.json"
$BATCH_URI = "system/batch"

module SlingUsers
  
  # a subclass of the library UserManager for creating fully featured groups
  # unlike the skeleton groups that super.create_group creates
  class FullGroupCreator < UserManager
    attr_reader :log, :file_log
    
    def initialize(sling)
      @sling = sling
      @sling.log.level = Logger::INFO
      #@sling.do_login
      #@user_manager = UserManager.new(@sling)
      super sling
      @log.level = Logger::INFO
      @file_log = Logger.new('load.log', 'daily')
      @file_log.level = Logger::INFO
    end
    
    # this method follows the series of POSTs that the UI makes to create a group with a 
    # full set of features including the initial sakai doce for Library and Participants
    def create_full_group(creator_id, groupname, title = nil, description = nil)
      creator = User.new(creator_id, "testuser")
      @sling.switch_user(creator)
      #POST 1 - creating the manager sub-group
      create_sub_group(groupname + "-manager", groupname + " Manager", description)
      
      #POST 2 - creating the member sub-group
      create_sub_group(groupname + "-member", groupname + " Member", description)
      
      #POST 3 creating the main group
      group = create_target_group(groupname, title, description)  #POST 3
      
      update_uri = "/#{$USERMANAGER_URI}group/"
      
      #POST 4 - updating the group managers
      batch_post = []
      batch_post[0] = {"url" => "#{update_uri}#{groupname}-member.update.json", "method" => "POST", "parameters" => {":manager" => "#{groupname}-manager","_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post[1] = {"url" => "#{update_uri}#{groupname}-manager.update.json", "method" => "POST", "parameters" => {":manager" => "#{groupname}-manager","_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post[2] = {"url" => "#{update_uri}#{groupname}.update.json", "method" => "POST", "parameters" => {":manager" => "#{groupname}-manager","_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post_json = JSON.generate batch_post
      @log.debug("POST 4 - updating the group managersbatch post is: #{batch_post_json}")
      @file_log.debug("POST 4 - updating the group managersbatch post is: #{batch_post_json}")
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("POST 4 - updating the group managers response code is: #{response.code}")
      @file_log.info("POST 4 - updating the group managers response code is: #{response.code}")
      
      #POST 5 - updating the group members
      batch_post = []
      batch_post[0] = {"url" => "#{update_uri}#{groupname}-manager.update.json", "method" => "POST", "parameters" => {":member" => "#{creator_id}", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post[1] = {"url" => "#{update_uri}#{groupname}.update.json", "method" => "POST", "parameters" => {":member" => "#{groupname}-member", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post[2] = {"url" => "#{update_uri}#{groupname}.update.json", "method" => "POST", "parameters" => {":member" => "#{groupname}-manager", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post_json = JSON.generate batch_post
      @log.debug("POST 5 - updating the group members batch post is: #{batch_post_json}")
      @file_log.debug("POST 5 - updating the group members batch post is: #{batch_post_json}")
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("POST 5 - updating the group members response code is: #{response.code}")
      @file_log.info("POST 5 - updating the group members response code is: #{response.code}")
      
      #POST 6 - creating test tags
      batch_post = []
      batch_post[0] = {"url" => "/tags/test-tag1", "method" => "POST", "parameters" => {"sakai:tag-name" => "test-tag1", "sling:resourceType" => "sakai/tag", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post[1] = {"url" => "/tags/test-tag2", "method" => "POST", "parameters" => {"sakai:tag-name" => "test-tag2", "sling:resourceType" => "sakai/tag", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post_json = JSON.generate batch_post
      @log.debug("POST 6 - creating test tags batch post is: #{batch_post_json}")
      @file_log.debug("POST 6 - creating test tags batch post is: #{batch_post_json}")      
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("POST 6 - creating test tags response code is: #{response.code}")
      @file_log.info("POST 6 - creating test tags response code is: #{response.code}")      
      
      #POST 7 - updating group visibility, joinability and permissions
      batch_post = []
      batch_post[0] = {"url" => "#{update_uri}#{groupname}.update.html", "method" => "POST", "parameters" => {"rep:group-viewers@Delete" => "", "sakai:group-visible" => "public", "sakai:group-joinable" => "yes", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post[1] = {"url" => "/~#{groupname}.modifyAce.html", "method" => "POST", "parameters" => {"principalId" => "everyone", "privilege@jcr:read" => "granted", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post[2] = {"url" => "/~#{groupname}.modifyAce.html", "method" => "POST", "parameters" => {"principalId" => "anonymous", "privilege@jcr:read" => "granted", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post_json = JSON.generate batch_post
      @log.debug("POST 7 - updating group visibility, joinability and permissions batch post is: #{batch_post_json}")
      @file_log.debug("POST 7 - updating group visibility, joinability and permissions batch post is: #{batch_post_json}")      
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("POST 7 - updating group visibility, joinability and permissions response code is: #{response.code}")
      @file_log.info("POST 7 - updating group visibility, joinability and permissions response code is: #{response.code}") 
      
      #POST 8 - creating initial sakai docs
      batch_post = []
      batch_post[0] = {"url" => "/system/pool/createfile", "method" => "POST", "parameters" => {"sakai:pooled-content-file-name" => "Library", "sakai:description" => "", "sakai:permissions" => "public", "sakai:copyright" => "creativecommons", \
                      "structure0" => "{\"library\":{\"_ref\":\"id9867543247\",\"_order\":0,\"_nonEditable\":true,\"_title\":\"Library\",\"main\":{\"_ref\":\"id9867543247\",\"_order\":0,\"_nonEditable\":true,\"_title\":\"Library\"}}}", \
                      "sakai:custom-mimetype" => "x-sakai/document","_charset_" => "utf-8"}, "_charset_" => "utf-8"}       
       
      batch_post[1] = {"url" => "/system/pool/createfile", "method" => "POST", "parameters" => {"sakai:pooled-content-file-name" => "Participants", "sakai:description" => "", "sakai:permissions" => "public", "sakai:copyright" => "creativecommons", \
                      "structure0" => "{\"participants\":{\"_ref\":\"id6573920372\",\"_order\":0,\"_nonEditable\":true,\"_title\":\"Participants\",\"main\":{\"_ref\":\"id6573920372\",\"_order\":0,\"_nonEditable\":true,\"_title\":\"Participants\"}}}", \
                      "sakai:custom-mimetype" => "x-sakai/document","_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post_json = JSON.generate batch_post
      @log.debug("#POST 8 - creating initial sakai docs batch post is: #{batch_post_json}")
      @file_log.debug("#POST 8 - creating initial sakai docs batch post is: #{batch_post_json}")      
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("POST 8 - creating initial sakai docs response code is: #{response.code}")
      @file_log.info("POST 8 - creating initial sakai docs response code is: #{response.code}") 
      ruby_body = JSON response.body
      results = ruby_body["results"]
      @log.debug("POST 8 - creating initial sakai docs results: #{results}")
      @file_log.debug("POST 8 - creating initial sakai docs results: #{results}")
      library_doc_hash, participants_doc_hash = nil, nil
      i = 0
      results.each do |result|
        result_body_json = JSON result["body"]
        content_item = result_body_json["_contentItem"]
        doc_hash = content_item["poolId"]
        content_item_name = content_item["item"]["sakai:pooled-content-file-name"]
        if ("Library".eql? content_item_name)
          library_doc_hash = doc_hash
        elsif ("Participants".eql? content_item_name)
          participants_doc_hash = doc_hash
        else
          @log.warn("could not find sakai doc name to confirm doc_hash")
        end
      end
      @log.info("POST 8 - creating initial sakai docs Library sakai doc hash: #{library_doc_hash}, Participants sakai doc hash #{participants_doc_hash}")
      @file_log.info("POST 8 - creating initial sakai docs Library sakai doc hash: #{library_doc_hash}, Participants sakai doc hash #{participants_doc_hash}")
 
      #POST 9 - importing sakai docs content
      batch_post = []
      batch_post[0] = {"url" => "/p/#{library_doc_hash}.resource", "method" => "POST", "parameters" => {":operation" => "import", ":contentType" => "json", ":replace" => "true", ":replaceProperties" => "true", \
                       ":content" => "{\"id9867543247\":{\"page\":\"<img id='widget_mylibrary_id1367865652332' class='widget_inline' style='display: block; padding: 10px; margin: 4px;' \
                       src='/devwidgets/mylibrary/images/mylibrary.png' data-mce-src='/devwidgets/mylibrary/images/mylibrary.png' data-mce-style='display: block; padding: 10px; margin: 4px;' border='1'><br></p>\"},\
                       \"id1367865652332\":{\"mylibrary\":{\"groupid\":\"#{groupname}\"}}}","_charset_" => "utf-8"}, "_charset_" => "utf-8"}       
       
       batch_post[1] = {"url" => "/p/#{participants_doc_hash}.resource", "method" => "POST", "parameters" => {":operation" => "import", ":contentType" => "json", ":replace" => "true", ":replaceProperties" => "true", \
                       ":content" => "{\"id6573920372\":{\"page\":\"<img id='widget_participants_id439704665' class='widget_inline' style='display: block; padding: 10px; margin: 4px;' src='/devwidgets/participants/images/participants.png' \
                       data-mce-src='/devwidgets/participants/images/participants.png' data-mce-style='display: block; padding: 10px; margin: 4px;' border='1'><br></p>\"}, \
                       \"id439704665\":{\"participants\":{\"groupid\":\"#{groupname}\"}}}","_charset_" => "utf-8"}, "_charset_" => "utf-8"}       
      batch_post_json = JSON.generate batch_post
      @log.debug("POST 9 - importing sakai docs content batch post is: #{batch_post_json}")
      @file_log.debug("POST 9 - importing sakai docs content batch post is: #{batch_post_json}")      
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("POST 9 - importing sakai docs content response code is: #{response.code}")
      @file_log.info("POST 9 - importing sakai docs content response code is: #{response.code}") 
      ruby_body = JSON response.body
      results = ruby_body["results"]
      @log.debug("POST 9 - importing sakai docs content results from importing sakai docs post: #{results}")
      @file_log.debug("POST 9 - importing sakai docs content results from importing sakai docs post: #{results}")
      
      #POST 10 - applying the test tags
      batch_post = []
      batch_post[0] = {"url" => "/~#{groupname}/public/authprofile", "method" => "POST", "parameters" => {"key" => "/tags/test-tag1", ":operation" => "tag", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      batch_post[1] = {"url" => "/~#{groupname}/public/authprofile", "method" => "POST", "parameters" => {"key" => "/tags/test-tag2", ":operation" => "tag", "_charset_" => "utf-8"}, "_charset_" => "utf-8"}
      @log.debug("resource batch post is: #{batch_post}")
      batch_post_json = JSON.generate batch_post
      @log.debug("POST 10 - applying the test tags batch post is: #{batch_post_json}")
      @file_log.debug("POST 10 - applying the test tags batch post is: #{batch_post_json}")      
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("POST 10 - applying the test tags response code is: #{response.code}")
      @file_log.info("POST 10 - applying the test tags response code is: #{response.code}")       
      ruby_body = JSON response.body
      results = ruby_body["results"]
      @log.debug("POST 10 - applying the test tags results from :operation => tag post: #{results}")
      @file_log.debug("POST 10 - applying the test tags results from :operation => tag post: #{results}")
     
      
      #POST 11 - setting the global viewers and permissions on the sakai docs
      batch_post = []
      batch_post[0] = {"url" => "/p/#{library_doc_hash}.members.html", "method" => "POST", "parameters" => {":viewer" => ["everyone", "anonymous"]}}
      batch_post[1] = {"url" => "/p/#{library_doc_hash}.modifyAce.html", "method" => "POST", "parameters" => {"principalId" => ["everyone", "anonymous"], "privilege@jcr:read" => "granted"}}
      batch_post[2] = {"url" => "/p/#{participants_doc_hash}.members.html", "method" => "POST", "parameters" => {":viewer" => ["everyone", "anonymous"]}}
      batch_post[3] = {"url" => "/p/#{participants_doc_hash}.modifyAce.html", "method" => "POST", "parameters" => {"principalId" => ["everyone", "anonymous"], "privilege@jcr:read" => "granted"}}
      batch_post_json = JSON.generate batch_post
      @log.debug("POST 11 - setting the global viewers and permissions on the sakai docs batch post is: #{batch_post_json}")
      @file_log.debug("POST 11 - setting the global viewers and permissions on the sakai docs batch post is: #{batch_post_json}")      
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("POST 11 - setting the global viewers and permissions on the sakai docs response code is: #{response.code}")
      @file_log.info("POST 11 - setting the global viewers and permissions on the sakai docs response code is: #{response.code}")      
      ruby_body = JSON response.body
      results = ruby_body["results"]
      @log.debug("POST 11 - setting the global viewers and permissions on the sakai docs results from setting permissions on sakai docs #{results}")
      @file_log.debug("POST 11 - setting the global viewers and permissions on the sakai docs results from setting permissions on sakai docs #{results}")
         
      #POST 12 - setting the member viewer and manager viewer for the sakai docs
      batch_post = []
      batch_post[0] = {"url" => "/p/#{library_doc_hash}.members.html", "method" => "POST", "parameters" => {":viewer" => "#{groupname}-member", "_charset_" =>"utf-8"},"_charset_" => "utf-8"}
      batch_post[1] = {"url" => "/p/#{library_doc_hash}.members.html", "method" => "POST", "parameters" => {":manager" => "#{groupname}-manager", "_charset_" =>"utf-8"},"_charset_" => "utf-8"}
      batch_post[2] = {"url" => "/p/#{participants_doc_hash}.members.html", "method" => "POST", "parameters" => {":viewer" => "#{groupname}-member", "_charset_" =>"utf-8"},"_charset_" => "utf-8"}
      batch_post[3] = {"url" => "/p/#{participants_doc_hash}.members.html", "method" => "POST", "parameters" => {":manager" => "#{groupname}-manager", "_charset_" =>"utf-8"},"_charset_" => "utf-8"}
      batch_post_json = JSON.generate batch_post
      @log.debug("POST 12 - setting the member viewer and manager viewer for the sakai docs batch post is: #{batch_post_json}")
      @file_log.debug("POST 12 - setting the member viewer and manager viewer for the sakai docs batch post is: #{batch_post_json}")      
      parameters = {"requests" => batch_post_json}
      response = @sling.execute_post(@sling.url_for("#{$BATCH_URI}"), parameters)
      @log.info("POST 12 - setting the member viewer and manager viewer for the sakai docs response code is: #{response.code}")
      @file_log.info("POST 12 - setting the member viewer and manager viewer for the sakai docs response code is: #{response.code}")        
      ruby_body = JSON response.body
      results = response.body["results"]
      @log.debug("POST 12 - setting the member viewer and manager viewer for the sakai docs results from setting viewer and manager on sakai docs #{results}")
      @file_log.debug("POST 12 - setting the member viewer and manager viewer for the sakai docs results from setting viewer and manager on sakai docs #{results}")
      
      #POST 13 - setting the doc structure on the sakai docs
      struct0 = {}    
      str = "{\"library\":{\"_title\":\"Library\",\"_order\":0,\"_nonEditable\":true,\"_view\":\"[\\\"everyone\\\",\\\"anonymous\\\",\\\"-member\\\"]\",\"_edit\":\"[\\\"-manager\\\"]\",\"_pid\":\"#{library_doc_hash}\"},\"participants\":{\"_title\":\"Participants\",\"_order\":1,\"_nonEditable\":true,\"_view\":\"[\\\"everyone\\\",\\\"anonymous\\\",\\\"-member\\\"]\",\"_edit\":\"[\\\"-manager\\\"]\",\"_pid\":\"#{participants_doc_hash}\"}}"
      struct0["structure0"] = str
      params = {}
      params[":content"] = JSON.generate struct0
      params[":contentType"] = "json"
      params[":operation"] = "import"
      params[":replace"] = true
      params[":replaceProperties"] = true
      params["_charset_"] = "utf-8"
      @log.debug("POST 13 - setting the doc structure on the sakai docs post params are: " + params.inspect)
      @file_log.debug("POST 13 - setting the doc structure on the sakai docs post params are: " + params.inspect)      
      uri = "/~#{groupname}/docstructure"
      response = @sling.execute_post(@sling.url_for(uri), params)
      #this is an html response
      @log.info("POST 13 - setting the doc structure on the sakai docs response code: #{response.code}")
      @file_log.info("POST 13 - setting the doc structure on the sakai docs response code: #{response.code}")
      
      # return the group that was created in create_target_group
      return group
    end
    
    # create the manager and member pseudo subgroups
    def create_sub_group(groupname, title, description)
      params = { ":name" => groupname }
      params["sakai:excludeSearch"] = true
      params["sakai:group-description"] = description || ""
      params["sakai:group-id"] = groupname
      params["sakai:group-title"] = title
      response = @sling.execute_post(@sling.url_for($GROUP_URI), params)
      @log.info("create_sub_group() for #{groupname} POST response code: #{response.code}")
      @log.debug("create_sub_group() for #{groupname} POST response body: #{response.body}")
      @file_log.info("create_sub_group() for #{groupname} POST response code: #{response.code}")
      @file_log.debug("create_sub_group() for #{groupname} POST response body: #{response.body}")
      if (response.code.to_i > 299)
        @log.warn("create_sub_group() returned #{response.code} group may already exist?")
        @file_log.warn("create_sub_group() returned #{response.code} group may already exist?")
      end
    end
    
    # create the group itself
    def create_target_group(groupname, title, description)
      params = { ":name" => groupname }
      params["sakai:category"] = "group"
      params["sakai:group-description"] = description || ""
      params["sakai:group-id"] = groupname
      params["sakai:group-title"] = title || ""
      params["sakai:joinRole"] = "member"
      params["sakai:roles"] = '[{"id":"member","title":"Member","allowManage":false},{"id":"manager","title":"Manager","allowManage":true}]'
      params["sakai:templateid"] = "simplegroup"
      response = @sling.execute_post(@sling.url_for($GROUP_URI), params)
      @log.info("create_target_group() for #{groupname} POST response code: #{response.code}")
      @log.debug("create_target_group() for #{groupname} POST response body: #{response.body}")
      @file_log.info("create_target_group() for #{groupname} POST response code: #{response.code}")
      @file_log.debug("create_target_group() for #{groupname} POST response body: #{response.body}")
      if (response.code.to_i > 299)
        @log.warn("create_target_group() returned #{response.code} group may already exist?")
        @file_log.warn("create_target_group() returned #{response.code} group may already exist?")
      end
      group = Group.new(groupname)
      return group
    end
    
  end
  
  if ($PROGRAM_NAME.include? 'full_group_creator.rb')
    @sling = Sling.new("http://localhost:8080/", false)
    @sling.log.level = Logger::DEBUG
    fgc = SlingUsers::FullGroupCreator.new @sling
    fgc.log.level = DEBUG
    fgc.file_log.level = DEBUG
    fgc.create_full_group "bp7742", "test-1881-group8", "test-1881-group8 Title", "test-1881-group8 Description"
  end 
end