#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
include SlingInterface
include SlingUsers

class TC_NodeCreateTest < Test::Unit::TestCase
  include SlingTest



  def test_structure_pool_item_file
    testpath = "/system/pool/createfile"
    res = @s.execute_post(@s.url_for(testpath), "a" => "foo", "b" => "bar")
	assert_equal("201",res.code)
	jsonResponse = JSON.parse(res.body)
	poolId = jsonResponse['_contentItem']['poolId']
	
    props = @s.get_node_props("/p/#{poolId}")
    assert_equal("foo", props["a"], "Expected property to be set")
    assert_equal("bar", props["b"], "Expected property to be set")

	fBody = "File Number 2 Content"
    res = @s.execute_file_post(@s.url_for("/p/#{poolId}.resource.html"), "file", "fileNo1", fBody, "text/plain")
	assert_equal("201",res.code)
	jsonResponse = JSON.parse(res.body)
	resourceId = jsonResponse['fileNo1']['resourceId']
	assert_not_nil(resourceId)
	
	
    res = @s.execute_get(@s.url_for("/p/#{poolId}/#{resourceId}"))
	assert_equal("200",res.code)
	assert_equal(fBody,res.body)

	res = @s.execute_post(@s.url_for("/p/#{poolId}"), 
	    "structure0" => " { \"a\" : {\"file.txt\" : { \"_ref\" : #{resourceId} }}}",
		"structure1" => " { \"b\" : {\"fileZ.txt\" : { \"_ref\" : #{resourceId} }}}" )
	assert_equal("200",res.code)
	
    res = @s.execute_get(@s.url_for("/p/#{poolId}/0/a/file.txt"))
	assert_equal("200",res.code)
	assert_equal(fBody,res.body)
	
	    res = @s.execute_get(@s.url_for("/p/#{poolId}/1/b/fileZ.txt"))
	assert_equal("200",res.code)
	assert_equal(fBody,res.body)


  end


end
