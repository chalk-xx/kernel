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

end

Test::Unit::UI::Console::TestRunner.run(TC_NodeCreateTest)

