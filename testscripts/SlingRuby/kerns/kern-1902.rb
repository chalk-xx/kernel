#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
require 'sling/authz'
include SlingUsers
include SlingFile
include SlingAuthz

class TC_Kern1902Test < Test::Unit::TestCase
  include SlingTest


  def test_check_ids
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
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Unable to get the metadata for the resource ")
    json = JSON.parse(res.body)
	contentItemId = json['_id']
	beforeMetaData = res.body


	# Update the pooled file
    res = @s.execute_post("#{contentpath}.html", { "testing" => "testvalue" })
    assert_equal("200", res.code, " #{manager.name} should have been granted write to #{contentpath} ")
	
	# perform annother get operation
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Unable to get the metadata for the resource ")
    json = JSON.parse(res.body)
	afterMetaData = res.body
    assert_equal(json["testing"], "testvalue", "Looks like the property was not written Got #{res.body}")
	assert_equal(json["_id"],contentItemId,"Content ID changed on update. \nBefore #{beforeMetaData} \nAfter #{afterMetaData} ")
	
	 wait_for_indexer()
	
    res = @fm.upload_pooled_file("random-#{m}.txt", "Plain content Updated", "text/plain", contentid)
    assert_equal("200", res.code, "Expected to be able to update pooled content #{res.body}")
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Unable to get the metadata for the resource ")
    json = JSON.parse(res.body)
	afterMetaData = res.body
    assert_equal(json["testing"], "testvalue", "Looks like the property was not written Got #{res.body}")
	assert_equal(json["_id"],contentItemId,"Content ID changed on update. \nBefore #{beforeMetaData} \nAfter #{afterMetaData} ")

    
  end

end
