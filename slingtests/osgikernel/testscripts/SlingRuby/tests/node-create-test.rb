#!/usr/bin/env ruby

require 'sling/sling'
require 'sling/test'
require 'test/unit/ui/console/testrunner.rb'
include SlingInterface
include SlingUsers
include SlingSites

class TC_NodeCreateTest < SlingTest

  def test_create_node
    testpath = "test/path"
    create_node(testpath, "a" => "foo", "b" => "bar")
    props = @s.get_node_props(testpath)
    assert_equal("foo", props["a"], "Expected property to be set")
    assert_equal("bar", props["b"], "Expected property to be set")
  end

  def test_create_file_node
    filedata = "<html><head><title>fish</title></head><body><p>cat</p></body></html>"
    filepath = "test/filepath"
    create_file_node(filepath, "file", filedata)
    res = @s.execute_get(@s.url_for(filepath + "/file"))
    assert_equal(200, res.code.to_i, "Expected GET to succeed")
    assert_equal(filedata, res.body, "Expected body back unmodified")
  end

  def test_create_file_node_and_version
    filedata = "<html><head><title>fish</title></head><body><p>cat</p></body></html>"
    filepath = "test/filepath"
    create_file_node(filepath, "file", filedata)
    res = @s.execute_get(@s.url_for(filepath + "/file"))
    assert_equal(200, res.code.to_i, "Expected GET to succeed")
    assert_equal(filedata, res.body, "Expected body back unmodified")
	puts("Attempting version operation ")
    res = @s.execute_post(@s.url_for(filepath + "/file.save.html"), "dummy")
    assert_equal(200, res.code.to_i, "Expected POST to save to succeed, looks like versioning is not working check the logs. "+res.body)
	puts(res.body)
	
	puts("Attempting To List Versions ")
    res = @s.execute_get(@s.url_for(filepath + "/file.versions.json"), "dummy")
    assert_equal(200, res.code.to_i, "Expected GET to versions to succeed, looks like versioning is not working check the logs. "+res.body)
	puts(res.body)
	
    filedata = "<html><head><title>fishfingers</title></head><body><p>cat</p></body></html>"
    filepath = "test/filepath"
    create_file_node(filepath, "file", filedata)
    res = @s.execute_get(@s.url_for(filepath + "/file"))
    assert_equal(200, res.code.to_i, "Expected GET to of second version succeed")
    assert_equal(filedata, res.body, "Expected body back unmodified")
    res = @s.execute_get(@s.url_for(filepath + "/file.versions.json"), "dummy")
    assert_equal(200, res.code.to_i, "Expected GET to versions to succeed, looks like versioning is not working check the logs. "+res.body)
	puts(res.body)
  end
  
  def test_create_file_node_and_get_version_history
    filedata = "<html><head><title>fish</title></head><body><p>cat</p></body></html>"
    filepath = "test/filepath"
    create_file_node(filepath, "file", filedata)
    res = @s.execute_get(@s.url_for(filepath + "/file"))
    assert_equal(200, res.code.to_i, "Expected GET to succeed")
    assert_equal(filedata, res.body, "Expected body back unmodified")
	puts("Attempting version history operation ")
    res = @s.execute_get(@s.url_for(filepath + "/file.versions.json"), "dummy")
    assert_equal(200, res.code.to_i, "Expected GET to versions to succeed, looks like versioning is not working check the logs. "+res.body)
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_NodeCreateTest)

