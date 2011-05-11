#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/users'
require 'test/unit.rb'
include SlingUsers
include SlingFile

class TC_Kern1829Test < Test::Unit::TestCase
  include SlingTest

  #
  # These are test cases for  KERN-1829
  #


  def setup
    super
    @fm = FileManager.new(@s)
    @um = UserManager.new(@s)
  end

  def test_manager_users
    m = Time.now.to_i.to_s

    # Create some users
    creator = create_user("testuser-#{m}")

    # Upload a file
    @s.switch_user(creator)
    res = @fm.upload_pooled_file('random.txt', '1', 'text/plain')
    file = JSON.parse(res.body)
    id = file['random.txt']
    url = @fm.url_for_pooled_file(id)
    # create a comments message store
    res = @s.execute_post("#{url}/comments/message", { "sling:resourceType" => "sakai/messagestore" })
    assert_equal("201", res.code)
    res = @s.execute_post("#{url}/comments", { 
            ":operation" => "import", 
            ":contentType" => "json", 
            ":replaceProperties" => true, 
            ":replace" => true, 
            ":content" => '{"comments" : [],"perPage":10, "direction":"comments_FirstUp", '+
               '"sakai:allowanonymous":true, "sakai:forcename":false,  "sakai:forcemail":false, '+
               '"sakai:notification":false, "sakai:notificationaddress":"user70", '+
               '"sling:resourceType":"sakai/settings",  "sakai:marker":"596725146", '+ 
               '"sakai:type":"comment", "_charset_":"utf-8"  }' })
    assert_equal("201", res.code)
    res = @s.execute_post("#{url}/comments/message.create.html", { 
            "sakai:type" => "comment",
            "sakai:to" => "internal:/p/#{id}/comments/message",
            "sakai:marker" => "596725146",
            "sakai:subject" => "Comment on /p/#{id}",
            "sakai:body" => "asdf",
            "sakai:messagebox" => "outbox",
            "sakai:sendstate" => "pending",
            "_charset_" => "utf-8" })

    assert_equal("200", res.code)
    sleep(2)
    res = @s.execute_get("#{url}/comments.tidy.-1.json")
    assert_equal("200", res.code)
    puts(res.body)

  end



end
