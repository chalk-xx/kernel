#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require 'ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
include SlingInterface
include SlingUsers

class TC_NodeCreateTest < Test::Unit::TestCase
  include SlingTest

  def test_create_pool_item
    testpath = "/system/pool/createfile"
    res = @s.execute_post(@s.url_for(testpath), "a" => "foo", "b" => "bar")
	assert_equal("201",res.code)
	jsonResponse = JSON.parse(res.body)
	poolId = jsonResponse['_contentItem']['poolId']
	
    props = @s.get_node_props("/p/#{poolId}.json")
    assert_equal("foo", props["a"], "Expected property to be set")
    assert_equal("bar", props["b"], "Expected property to be set")
  end

  def test_update_pool_item
    testpath = "/system/pool/createfile"
    res = @s.execute_post(@s.url_for(testpath), "a" => "foo", "b" => "bar")
	assert_equal("201",res.code)
	jsonResponse = JSON.parse(res.body)
	poolId = jsonResponse['_contentItem']['poolId']
	
    props = @s.get_node_props("/p/#{poolId}.json")
    assert_equal("foo", props["a"], "Expected property to be set")
    assert_equal("bar", props["b"], "Expected property to be set")

    res = @s.execute_post(@s.url_for("/p/#{poolId}"), "a" => "foobar", "b" => "barfoo")
	assert_equal("200",res.code)
    props = @s.get_node_props("/p/#{poolId}.json")
    assert_equal("foobar", props["a"], "Expected property to be set")
    assert_equal("barfoo", props["b"], "Expected property to be set")

  end

  def test_addchild_pool_item
    testpath = "/system/pool/createfile"
    res = @s.execute_post(@s.url_for(testpath), "a" => "foo", "b" => "bar")
	assert_equal("201",res.code)
	jsonResponse = JSON.parse(res.body)
	poolId = jsonResponse['_contentItem']['poolId']
	
    props = @s.get_node_props("/p/#{poolId}.json")
    assert_equal("foo", props["a"], "Expected property to be set")
    assert_equal("bar", props["b"], "Expected property to be set")

    res = @s.execute_post(@s.url_for("/p/#{poolId}.resource.html"), "aa" => "foobar", "bb" => "barfoo")
	assert_equal("201",res.code)
	jsonResponse = JSON.parse(res.body)
	resourceId = jsonResponse['_contentItem']['resourceId']
	
    res = @s.execute_get(@s.url_for("/p/#{poolId}/#{resourceId}.json"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
    assert_equal("foobar", props["aa"], "Expected property to be set")
    assert_equal("barfoo", props["bb"], "Expected property to be set")

  end


  def test_addchild_pool_item
    testpath = "/system/pool/createfile"
    res = @s.execute_post(@s.url_for(testpath), "a" => "foo", "b" => "bar")
	assert_equal("201",res.code);
	jsonResponse = JSON.parse(res.body)
	poolId = jsonResponse['_contentItem']['poolId']
	
    props = @s.get_node_props("/p/#{poolId}.json")
    assert_equal("foo", props["a"], "Expected property to be set")
    assert_equal("bar", props["b"], "Expected property to be set")

    res = @s.execute_post(@s.url_for("/p/#{poolId}.resource.html"), "aa" => "foobar", "bb" => "barfoo")
	assert_equal("201",res.code)
	jsonResponse = JSON.parse(res.body)
	resourceId = jsonResponse['_contentItem']['resourceId']
	
    res = @s.execute_get(@s.url_for("/p/#{poolId}/#{resourceId}.json"))
	assert_equal("200",res.code)
	props = JSON.parse(res.body)
    assert_equal("foobar", props["aa"], "Expected property to be set")
    assert_equal("barfoo", props["bb"], "Expected property to be set")

  end


  def test_updatechild_pool_item_file
    testpath = "/system/pool/createfile"
    res = @s.execute_post(@s.url_for(testpath), "a" => "foo", "b" => "bar")
	assert_equal("201",res.code)
	jsonResponse = JSON.parse(res.body)
	poolId = jsonResponse['_contentItem']['poolId']
	
    props = @s.get_node_props("/p/#{poolId}.json")
    assert_equal("foo", props["a"], "Expected property to be set")
    assert_equal("bar", props["b"], "Expected property to be set")

	fBody = "File Number 1 Content"
    res = @s.execute_file_post(@s.url_for("/p/#{poolId}.resource.html"), "file", "fileNo1", fBody, "text/plain")
	assert_equal("201",res.code)
	jsonResponse = JSON.parse(res.body)
	resourceId = jsonResponse['fileNo1']['resourceId']
	assert_not_nil(resourceId)
	
	
    res = @s.execute_get(@s.url_for("/p/#{poolId}/#{resourceId}"))
	assert_equal("200",res.code)
	assert_equal(fBody,res.body)

  end


end


