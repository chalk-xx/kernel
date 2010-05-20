#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

$testfile1 = "<html><head><title>KERN 312</title></head><body><p>Should work</p></body></html>"
$testfile2 = "<html><head><title>KERN 312</title></head><body><p>Should still work</p></body></html>"

class TC_Kern312Test < SlingTest

  def upload_file(nodename, data)
    n = create_file_node(nodename, "testfile", "testfile", data, "text/html")
    filepath = "#{nodename}/testfile"
    res = @s.execute_get(@s.url_for(filepath))
    assert_equal(data, res.body, "Expected content to upload cleanly")
    return filepath
  end  

  def test_save_uploaded_file
    m = Time.now.to_i.to_s
    nodename = "upload_test#{m}"
    filepath = upload_file(nodename, $testfile1)
    @s.save_node(filepath)
    versions = @s.versions(filepath)
    assert_equal(2, versions.size, "Expected two versions")
  end

  def test_replace_saved_file
    m = Time.now.to_i.to_s
    nodename = "upload_test#{m}"
    filepath = upload_file(nodename, $testfile1)
    @s.save_node(filepath)
    @s.save_node(filepath)
    versions = @s.versions(filepath)
    assert_equal(3, versions.size, "Expected three versions")
    upload_file(nodename, $testfile2)
    versions = @s.versions(filepath)
    assert_equal(3, versions.size, "Still expected three versions")
    @s.save_node(filepath)
  end

  def test_render_versions
    m = Time.now.to_i.to_s
    nodename = "upload_test#{m}"
    filepath = upload_file(nodename, $testfile1)
    @s.save_node(filepath)
    old_version = @s.save_node(filepath)
    versioned = @s.version(filepath, old_version, "")
    assert_equal($testfile1, versioned.body, "Expected version to render correctly")
    upload_file(nodename, $testfile2)
    new_version = @s.save_node(filepath)
    versioned = @s.version(filepath, new_version, "")
    assert_equal($testfile2, versioned.body, "Expected version to render correctly")
    versioned = @s.version(filepath, old_version, "")
    assert_equal($testfile1, versioned.body, "Expected version to render correctly")
  end

end

Test::Unit::UI::Console::TestRunner.run(TC_Kern312Test)

