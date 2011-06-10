#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'sling/test'
require 'sling/file'
include SlingUsers
include SlingFile

class TC_Kern1895Test < Test::Unit::TestCase
  include SlingTest


  def test_delete_pool_property
    @fm = FileManager.new(@s)
    m = Time.now.to_f.to_s.gsub('.', '')
    manager = create_user("user-manager-#{m}")
    @s.switch_user(manager)
    res = @fm.upload_pooled_file("random-#{m}.txt", "Plain content", "text/plain")
    assert_equal("201", res.code, "Expected to be able to create pooled content")
	uploadresult = JSON.parse(res.body)
	contentid = uploadresult["random-#{m}.txt"]['poolId']
	assert_not_nil(contentid, "Should have uploaded ID")
	contentpath = @s.url_for("/p/#{contentid}")

	# Create Modify a property.
    pres = @s.execute_post("#{contentpath}.html", { "testing" => "testvalue" })
    assert_equal("200", pres.code, " #{manager.name} should have been granted write to #{contentpath} ")
    @log.info(" #{manager.name} can write to the resource ")
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Unable to get the metadata for the resource ")
    m = JSON.parse(res.body)
    assert_equal( "testvalue",m["testing"], "Looks like the property was not written Got #{res.body}, \n #{pres.body}")

	#rewrite a property
    pres = @s.execute_post("#{contentpath}.html", { "testing" => "a" })
    assert_equal("200", pres.code, " #{manager.name} should have been granted write to #{contentpath} ")
    @log.info(" #{manager.name} can write to the resource ")
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Unable to get the metadata for the resource ")
    m = JSON.parse(res.body)
    assert_equal("a", m["testing"],  "Looks like the property was not written Got #{res.body}, \n #{pres.body}")


	#clear a property
    pres = @s.execute_post("#{contentpath}.html", { "testing" => "" })
    assert_equal("200", pres.code, " #{manager.name} should have been granted write to #{contentpath} ")
    @log.info(" #{manager.name} can write to the resource ")
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Unable to get the metadata for the resource ")
    m = JSON.parse(res.body)
    assert_nil(m["testing"],  "Looks like the property was not written Got #{res.body}, \n #{pres.body}")
	
	#clear a property
    pres = @s.execute_post("#{contentpath}.html", { "testing@Delete" => "" })
    assert_equal("200", pres.code, " #{manager.name} should have been granted write to #{contentpath} ")
    @log.info(" #{manager.name} can write to the resource ")
    res = @s.execute_get("#{contentpath}.json")
    assert_equal("200", res.code, "Unable to get the metadata for the resource ")
    m = JSON.parse(res.body)
    assert_nil(m["testing"], "Looks like the property was not written Got #{res.body}, \n #{pres.body}")
	
	
	
	
  end

end
