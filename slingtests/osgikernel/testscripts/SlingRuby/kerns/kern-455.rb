#!/usr/bin/env ruby

require 'sling/test'
require 'sling/search'
require 'test/unit.rb'
require 'test/unit/ui/console/testrunner.rb'
include SlingSearch

$testfile1 = "<html><head><title>KERN 312</title></head><body><p>Should work</p></body></html>"

#This kern tests wheter regular users are able to upload files into their private store.

class TC_Kern455Test < SlingTest

  def upload_file(nodename, data)
    n = create_file_node(nodename, "testfile", "testfile", data, "text/html")
    filepath = "#{nodename}/testfile"
    res = @s.execute_get(@s.url_for(filepath))
    assert_equal(data, res.body, "Expected content to upload cleanly")
    return filepath
  end  

  def test_save_uploaded_file
    m = Time.now.to_i.to_s
    dummyuser = create_user("dummyuser"+m)
    @s.switch_user(dummyuser)
    
    #@s.switch_user(SlingUsers::User.admin_user())
    
    nodename = "_user/private/upload_test#{m}"
    filepath = upload_file(nodename, $testfile1)
  end



end

Test::Unit::UI::Console::TestRunner.run(TC_Kern455Test)

