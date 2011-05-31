#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/authz'
include SlingUsers
include SlingFile
include SlingAuthz

class TC_Kern1877Test < Test::Unit::TestCase
  include SlingTest

  def add_activity(url, appid, templateid, messagetext, publicActivity)
    if ( publicActivity )
      res = @s.execute_post("#{url}.activity.json", {
        "sakai:activity-appid" => appid,
        "sakai:activity-templateid" => templateid,
        "sakai:activityMessage" => messagetext,
	    "sakai:activity-privacy" => "public"
      })
      assert_equal("200", res.code, "Should have added activity")
	else 
      res = @s.execute_post("#{url}.activity.json", {
        "sakai:activity-appid" => appid,
        "sakai:activity-templateid" => templateid,
        "sakai:activityMessage" => messagetext
      })
      assert_equal("200", res.code, "Should have added activity")
	end
  end

  def test_get_pooled_content_activities
    @fm = FileManager.new(@s)
    @authz = SlingAuthz::Authz.new(@s)
    m = Time.now.to_f.to_s.gsub('.', '')
    manager = create_user("user-manager-#{m}")
    @s.switch_user(manager)
    res = @fm.upload_pooled_file("random-#{m}.txt", "Plain content", "text/plain")
    assert_equal("201", res.code, "Expected to be able to create pooled content")
	uploadresult = JSON.parse(res.body)
	contentid = uploadresult["random-#{m}.txt"]['poolId']
	assert_not_nil(contentid, "Should have uploaded ID")
	contentpath = @s.url_for("/p/#{contentid}")

	@authz.grant("/p/#{contentid}","everyone","jcr:read" => "granted")
	@authz.grant("/p/#{contentid}","anonymous","jcr:read" => "granted")

	# Add three activity notes.
    res = @s.execute_post("#{contentpath}.html", { "testing" => "testvalue" })
    assert_equal("200", res.code, " #{manager.name} should have been granted write to #{contentpath} ")
    @log.info(" #{manager.name} can write to the resource ")
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Unable to get the metadata for the resource ")
    json = JSON.parse(res.body)
    assert_equal(json["testing"], "testvalue", "Looks like the property was not written Got #{res.body}")
    
	add_activity(contentpath, "status", "default", "First activity #{m}", false)
	add_activity(contentpath, "status", "default", "Second activity #{m}", true)
	add_activity(contentpath, "status", "default", "Third activity #{m}", false)

    wait_for_indexer()
	
	
    res = @s.execute_get(@s.url_for("/var/search/activity/all.tidy.json"))
    assert_equal("200", res.code, "Should have found activity feed")
    @log.info("Private Activity feed  is #{res.body}")
    activityfeed = JSON.parse(res.body)
	firstActivity = false
	secondActivity = false
	thirdActivity = false
	activityfeed["results"].each do |result|
	     if ( result["sakai:activityMessage"] == "First activity #{m}" )
			assert_equal("random-#{m}.txt",result["sakai:pooled-content-file-name"])
			firstActivity = true
	     elsif ( result["sakai:activityMessage"] == "Second activity #{m}" )
			assert_equal("random-#{m}.txt",result["sakai:pooled-content-file-name"])
			secondActivity = true
	     elsif ( result["sakai:activityMessage"] == "Third activity #{m}" )
			assert_equal("random-#{m}.txt",result["sakai:pooled-content-file-name"])
			thirdActivity = true
		 end
    end
	
    assert_equal(true,firstActivity)
    assert_equal(true,secondActivity)
    assert_equal(true,thirdActivity)

	
	@s.switch_user(User.anonymous)
	
    res = @s.execute_get(@s.url_for("/var/search/activity/all.tidy.json"))
    assert_equal("200", res.code, "Should have found activity feed")
    @log.info("Public Activity feed is #{res.body}")
    activityfeed = JSON.parse(res.body)
	firstActivity = false
	secondActivity = false
	thirdActivity = false
	activityfeed["results"].each do |result|
	     if ( result["sakai:activityMessage"] == "First activity #{m}" )
			assert_equal("random-#{m}.txt",result["sakai:pooled-content-file-name"])
			firstActivity = true
	     elsif ( result["sakai:activityMessage"] == "Second activity #{m}" )
			assert_equal("random-#{m}.txt",result["sakai:pooled-content-file-name"])
			secondActivity = true
	     elsif ( result["sakai:activityMessage"] == "Third activity #{m}" )
			assert_equal("random-#{m}.txt",result["sakai:pooled-content-file-name"])
			thirdActivity = true
		 end
    end
    assert_equal(false,firstActivity)
    assert_equal(true,secondActivity)
    assert_equal(false,thirdActivity)
  end

end
