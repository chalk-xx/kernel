#!/usr/bin/env ruby

# Add all files in testscripts\SlingRuby\lib directory to ruby "require" search path
require './ruby-lib-dir.rb'

require 'sling/sling'
require 'sling/test'
require 'sling/file'
require 'sling/message'
require 'test/unit.rb'
include SlingInterface
include SlingUsers
include SlingMessage
include SlingFile

class TC_MyFileTest_797 < Test::Unit::TestCase
  include SlingTest

  def setup
    super
    @ff = FileManager.new(@s)
  end

  def test_canModify
    m = Time.now.to_f.to_s.gsub('.', '')
    @siteid = "creator#{m}";
    creator = create_user("@siteid")

    # Upload a file to the user's public space.
    @s.switch_user(creator)
    resp = @s.execute_file_post(@s.url_for("/system/pool/createfile"), "alfa", "alfa", "This is some random content: alfaalfa.", "text/plain")
    assert_equal(201, resp.code.to_i(), "Expected to be able to upload a file.")
    uploadresult = JSON.parse(resp.body)
    poolId = uploadresult['alfa']
    assert_not_nil(poolId)    
    
    # Check canModify on uploaded file

    # terse output
    url = "/p/#{poolId}.canModify.json";
    resp = @s.execute_get(@s.url_for(url));
    assert_equal(200, resp.code.to_i, "Should be OK");
    json = JSON.parse(resp.body)
    # creator canModify own file
    assert_equal(true, json["/p/#{poolId}"])
    assert_equal(nil, json["privileges"])

    # verbose output
    url = "/p/#{poolId}.canModify.json?verbose=true";
    resp = @s.execute_get(@s.url_for(url));
    assert_equal(200, resp.code.to_i, "Should be OK");
    @log.info(resp.body)
    json = JSON.parse(resp.body)
    # creator canModify own file
    assert_equal(true, json["/p/#{poolId}"])
    assert_not_nil(json["privileges"])
    assert_equal(true, json["privileges"]["All"])

    # Check canModify on /var

    # terse output
    url = "/var.canModify.json";
    resp = @s.execute_get(@s.url_for(url));
    assert_equal(200, resp.code.to_i, "Should be OK");
    json = JSON.parse(resp.body)
    # creator cannot modify /var
    assert_equal(false, json["/var"])
    assert_equal(nil, json["privileges"])

    # verbose output
    url = "/var.canModify.json?verbose=true";
    resp = @s.execute_get(@s.url_for(url));
    assert_equal(200, resp.code.to_i, "Should be OK");
    json = JSON.parse(resp.body)
    # creator cannot modify /var
    assert_equal(false, json["/var"])
    assert_not_nil(json["privileges"])
    assert_equal(true, json["privileges"]["jcr:read"])
    
    # verify behavior against a regular resource (i.e. not pooled content)

    # verbose output
    @s.switch_user(SlingUsers::User.admin_user())
    url = "/.canModify.json?verbose=true";
    resp = @s.execute_get(@s.url_for(url));
    assert_equal(200, resp.code.to_i, "Should be OK");
    json = JSON.parse(resp.body)
    # admin can modify /
    assert_equal(true, json["/"])
    assert_not_nil(json["privileges"])
    assert_equal(true, json["privileges"]["jcr:all"])
    
    # verify 404 not found
    url = "/p/foo.canModify.json?verbose=true";
    resp = @s.execute_get(@s.url_for(url));
    assert_equal(404, resp.code.to_i, "Should be not found");
    
  end

  def teardown
    super
  end

end

