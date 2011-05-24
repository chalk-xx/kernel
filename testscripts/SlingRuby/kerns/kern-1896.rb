#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/users'
require 'test/unit.rb'
include SlingUsers
include SlingFile

class TC_Kern1896Test < Test::Unit::TestCase
  include SlingTest

  #
  # These are test cases for  KERN-1896
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
    id = file['random.txt']['poolId']
    url = @fm.url_for_pooled_file(id)
    # create a comments message store
    res = @s.execute_post("#{url}/id4361077/discussion/message", { "sling:resourceType" => "sakai/messagestore" })
    assert_equal("201", res.code)
    res = @s.execute_post("#{url}/id4361077/discussion", { 
            ":operation" => "import", 
            ":contentType" => "json", 
            ":replaceProperties" => true, 
            ":replace" => true, 
            ":content" => '{"sakai:whocanaddtopic":"managers_and_members","sakai:whocanreply":"everyone","marker":"id4361077"} ' })
    assert_equal("201", res.code)
    res = @s.execute_post("#{url}/id4361077/discussion/message.create.html", {
            "sakai:initialpost" => "true", 
            "sakai:writeto" => "/p/#{id}/id4361077/discussion/message", 
            "sling:resourceType" => "sakai/message",             
            "sakai:type" => "discussion",
            "sakai:to" => "discussion:/p/#{id}/id4361077/discussion/message",
            "sakai:marker" => "id4361077",
            "sakai:subject" => "Topic #{id}",
            "sakai:body" => "Message",
            "sakai:messagebox" => "pending",
            "sakai:sendstate" => "pending",
            "_charset_" => "utf-8" })

    assert_equal("200", res.code)
    sleep(2)
    res = @s.execute_get("#{url}/id4361077/discussion.tidy.-1.json")
    assert_equal("200", res.code)
	@log.error("Got #{res.body}")
    
    json = JSON.parse(res.body)
    checkPath("#{id}/id4361077/discussion", json)

    wait_for_indexer()

    res = @s.execute_get("#{url}/id4361077/discussion.tidy.-1.json")

    res = @s.execute_get(@s.url_for("/var/search/discussions/threaded.json?path=/p/#{id}/id4361077/discussion/message&marker=id4361077&_charset_=utf-8&_=1306183258586"))
	assert_equal("200", res.code)
	@log.error("Got #{res.body}")
    json = JSON.parse(res.body)
	assert_equal(1, json["total"])
	assert_equal("Comment on /p/#{id}",json["results"][0]["post"]["sakai:subject"])

  end

  def checkPath(path, json) 
      json.each_pair do | k,v |
          if v.is_a?(Hash)
             checkPath("#{path}/#{k}",v)
          end
      end
      assert_equal(json["_path"], path)
  end

end
